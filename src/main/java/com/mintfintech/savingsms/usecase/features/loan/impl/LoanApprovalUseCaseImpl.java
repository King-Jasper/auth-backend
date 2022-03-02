package com.mintfintech.savingsms.usecase.features.loan.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.corebankingservice.LoanApplicationRequestCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.LoanApplicationResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.NewLoanAccountResponseCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.events.outgoing.LoanApprovalEmailEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.LoanDeclineEmailEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.PushNotificationEvent;
import com.mintfintech.savingsms.usecase.data.response.LoanManager;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.features.loan.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.features.loan.LoanApprovalUseCase;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeFieldType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanApprovalUseCaseImpl implements LoanApprovalUseCase {

    private final LoanRequestEntityDao loanRequestEntityDao;
    private final CoreBankingServiceClient coreBankingServiceClient;
    private final ApplicationProperty applicationProperty;
    private final GetLoansUseCase getLoansUseCase;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final EmployeeInformationEntityDao employeeInformationEntityDao;
    private final CustomerLoanProfileEntityDao customerLoanProfileEntityDao;
    private final AppUserEntityDao appUserEntityDao;
    private final ApplicationEventService applicationEventService;
    private final LoanTransactionEntityDao loanTransactionEntityDao;
    private final SystemIssueLogService systemIssueLogService;
    private final LoanReviewLogEntityDao loanReviewLogEntityDao;

    @Transactional
    @Override
    public LoanModel approveLoanRequest(AuthenticatedUser authenticatedUser, String loanId, String reason, boolean approved) {

        LoanManager loanManager = LoanManager.getManager(authenticatedUser);

        LoanRequestEntity loanRequestEntity = loanRequestEntityDao.findByLoanId(loanId)
                .orElseThrow(() -> new BadRequestException("Loan request for this loanId " + loanId + " does not exist"));

        if (loanRequestEntity.getApprovalStatus() != ApprovalStatusConstant.PENDING) {
            throw new BadRequestException("This loan request is not in pending state");
        }
        AppUserEntity appUser = appUserEntityDao.getRecordById(loanRequestEntity.getRequestedBy().getId());

        if (approved) {
            processLoanApproval(loanManager, loanRequestEntity, appUser);
        } else {
            processLoanDecline(loanManager, loanRequestEntity, appUser, reason);
        }

        return getLoansUseCase.toLoanModel(loanRequestEntity);
    }

    @Override
    public void processApprovedLoans() {

        List<LoanRequestEntity> loans = loanRequestEntityDao.getApprovedLoansWithNoAccountNumber();

        for (LoanRequestEntity loan : loans) {
            MsClientResponse<NewLoanAccountResponseCBS> msClientResponse = coreBankingServiceClient.getLoanAccountDetails(loan.getTrackingReference());

            if (msClientResponse.isSuccess() && StringUtils.isNotEmpty(msClientResponse.getData().getAccountNumber())) {

                NewLoanAccountResponseCBS responseCBS = msClientResponse.getData();
                loan.setBankOneAccountNumber(responseCBS.getAccountNumber());
                loan.setApprovalStatus(ApprovalStatusConstant.DISBURSED);
                loanRequestEntityDao.saveRecord(loan);

                LoanTransactionEntity transaction = LoanTransactionEntity.builder()
                        .loanRequest(loan)
                        .transactionAmount(loan.getLoanAmount())
                        .status(TransactionStatusConstant.SUCCESSFUL)
                        .transactionType(TransactionTypeConstant.CREDIT)
                        .transactionReference(loan.getTrackingReference())
                        .build();
                loanTransactionEntityDao.saveRecord(transaction);

                AppUserEntity debtor = appUserEntityDao.getRecordById(loan.getRequestedBy().getId());
                String text = "Hi "+debtor.getFirstName()+", your loan request has been disbursed to your account. Thanks for choosing Mintyn.";
                PushNotificationEvent pushNotificationEvent = new PushNotificationEvent("New Savings", text, debtor.getDeviceGcmNotificationToken());
                pushNotificationEvent.setUserId(debtor.getUserId());
                applicationEventService.publishEvent(ApplicationEventService.EventType.PUSH_NOTIFICATION_TOKEN, new EventModel<>(pushNotificationEvent));
            }
        }
    }

    private void processLoanApproval(LoanManager loanManager, LoanRequestEntity loanRequest, AppUserEntity appUser) {

        if (loanRequest.getLoanType() == LoanTypeConstant.PAYDAY) {

            CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser)
                    .orElseThrow(() -> new NotFoundException("No Loan Customer Profile Exists for this User"));

            EmployeeInformationEntity employeeInfo = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());
            if (employeeInfo.getVerificationStatus() != ApprovalStatusConstant.APPROVED) {
                throw new BadRequestException("Employment Information have not been verified for this user");
            }
        }

        boolean userReviewed = loanReviewLogEntityDao.recordExistForUserIdAndEntityId(loanManager.getReviewerUserId(), loanRequest.getId());

        if(userReviewed) {
            throw new BusinessLogicConflictException("Sorry, you have already reviewed this loan.");
        }
        LoanReviewLogEntity reviewLogEntity = LoanReviewLogEntity.builder()
                .reviewerName(loanManager.getReviewerName())
                .entityId(loanRequest.getId())
                .reviewLogType(LoanReviewLogType.LOAN_REQUEST)
                .reviewerId(loanManager.getReviewerUserId())
                .build();

        String description = "";
        if(loanRequest.getReviewStage() == null || loanRequest.getReviewStage() == LoanReviewStageConstant.FIRST_REVIEW) {
            if(!loanManager.isFinanceOfficer()) {
                throw new BusinessLogicConflictException("Request aborted. Only a finance officer can approve this request.");
            }
            description = "Loan approved by Finance officer";
            loanRequest.setReviewStage(LoanReviewStageConstant.SECOND_REVIEW);
            loanRequestEntityDao.saveRecord(loanRequest);

            reviewLogEntity.setDescription(description);
            loanReviewLogEntityDao.saveRecord(reviewLogEntity);
            return;
        }
        if(loanRequest.getReviewStage() == LoanReviewStageConstant.SECOND_REVIEW) {
            if(!loanManager.isBusinessManager()) {
                throw new BusinessLogicConflictException("Request aborted. Only a business manager can approve this request.");
            }
            if(loanRequest.getLoanType() == LoanTypeConstant.BUSINESS) {
                description = "Loan approved by Business manager";
                loanRequest.setReviewStage(LoanReviewStageConstant.THIRD_REVIEW);
                loanRequestEntityDao.saveRecord(loanRequest);

                reviewLogEntity.setDescription(description);
                loanReviewLogEntityDao.saveRecord(reviewLogEntity);
                return;
            }
        }
        if(loanRequest.getReviewStage() == LoanReviewStageConstant.THIRD_REVIEW) {
            if(!loanManager.isBusinessManager()) {
                throw new BusinessLogicConflictException("Request aborted. Only a business manager can approve this request.");
            }
        }

        description = "Loan approved by business manager";
        reviewLogEntity.setDescription(description);
        loanReviewLogEntityDao.saveRecord(reviewLogEntity);

        MintBankAccountEntity bankAccount = mintBankAccountEntityDao.getRecordById(loanRequest.getBankAccount().getId());

        LoanApplicationRequestCBS request = LoanApplicationRequestCBS.builder()
                .loanType(loanRequest.getLoanType().name())
                .accountNumber(bankAccount.getAccountNumber())
                .amount(loanRequest.getLoanAmount().intValue())
                .build();
        if(loanRequest.getLoanType() == LoanTypeConstant.BUSINESS) {
            request.setDurationInMonths(loanRequest.getDurationInMonths());
        }

        MsClientResponse<LoanApplicationResponseCBS> msClientResponse = coreBankingServiceClient.createLoanApplication(request);

        if (msClientResponse.getStatusCode() == HttpStatus.OK.value()
                && msClientResponse.isSuccess()
                && StringUtils.isNotEmpty(msClientResponse.getData().getTrackingReference())
                && msClientResponse.getData().isSuccess()
        ) {
            LoanApplicationResponseCBS responseCBS = msClientResponse.getData();

            LocalDateTime repaymentDate;
            if(loanRequest.getLoanType() == LoanTypeConstant.BUSINESS) {
                repaymentDate = LocalDateTime.now().plusMonths(loanRequest.getDurationInMonths());
            }else {
                repaymentDate = LocalDateTime.now().plusDays(applicationProperty.getPayDayLoanMaxTenor());
            }
            DayOfWeek dayOfWeek = repaymentDate.getDayOfWeek();
            if(dayOfWeek == DayOfWeek.SATURDAY) {
                repaymentDate = repaymentDate.plusDays(3);
            }else if(dayOfWeek == DayOfWeek.SUNDAY) {
                repaymentDate = repaymentDate.plusDays(2);
            }
            loanRequest.setApprovalStatus(ApprovalStatusConstant.APPROVED);
            loanRequest.setApprovedDate(LocalDateTime.now());
            loanRequest.setApproveByName(loanManager.getReviewerName());
            loanRequest.setApproveByUserId(loanManager.getReviewerUserId());
            loanRequest.setTrackingReference(responseCBS.getTrackingReference());
            loanRequest.setRepaymentDueDate(repaymentDate);
            loanRequestEntityDao.saveRecord(loanRequest);

            MintAccountEntity mintAccount = mintAccountEntityDao.getRecordById(bankAccount.getMintAccount().getId());
            if (StringUtils.isEmpty(mintAccount.getBankOneCustomerId())) {
                mintAccount.setBankOneCustomerId(responseCBS.getCustomerId());
                mintAccountEntityDao.saveRecord(mintAccount);
            }
            sendLoanApprovalEmail(loanRequest, appUser);
            //TODO implement sending of loan document.
        } else {
            String message = String.format("Loan Id: %s; message: %s", loanRequest.getLoanId(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Loan Creation Failure", "Loan Creation Failed", message);

            throw new BusinessLogicConflictException("Unable to approve loan at the moment. Please try again later.");
        }
    }

    private void processLoanDecline(LoanManager loanManager, LoanRequestEntity loanRequest, AppUserEntity appUser, String reason) {

        LoanReviewLogEntity reviewLogEntity = LoanReviewLogEntity.builder()
                .reviewerName(loanManager.getReviewerName())
                .entityId(loanRequest.getId())
                .reviewLogType(LoanReviewLogType.LOAN_REQUEST)
                .reviewerId(loanManager.getReviewerUserId())
                .build();

        String description = "";
        if(loanRequest.getReviewStage() == null || loanRequest.getReviewStage() == LoanReviewStageConstant.FIRST_REVIEW) {
            if (!loanManager.isFinanceOfficer()) {
                throw new BusinessLogicConflictException("Request aborted. Only a finance officer can decline this request at the current stage.");
            }
            description = "Loan declined by Finance officer";
        }else if(loanRequest.getReviewStage() == LoanReviewStageConstant.SECOND_REVIEW) {
            if (!loanManager.isBusinessManager()) {
                throw new BusinessLogicConflictException("Request aborted. Only a business manager can decline this request at the current stage.");
            }
            description = "Loan declined by Business manager";
        }

        reviewLogEntity.setDescription(description);
        loanReviewLogEntityDao.saveRecord(reviewLogEntity);


        loanRequest.setApprovalStatus(ApprovalStatusConstant.DECLINED);
        loanRequest.setRepaymentStatus(LoanRepaymentStatusConstant.CANCELLED);
        loanRequest.setApproveByName(loanManager.getReviewerName());
        loanRequest.setApproveByUserId(loanManager.getReviewerUserId());
        loanRequest.setActiveLoan(false);
        loanRequest.setRejectionReason(reason);

        loanRequestEntityDao.saveRecord(loanRequest);

        LoanDeclineEmailEvent event = LoanDeclineEmailEvent.builder()
                .customerName(appUser.getName())
                .recipient(appUser.getEmail())
                .loanAmount(loanRequest.getLoanAmount())
                .reason(reason)
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_REQUEST_DECLINED, new EventModel<>(event));
    }

    private void sendLoanApprovalEmail(LoanRequestEntity loanRequest, AppUserEntity appUser) {

        LoanApprovalEmailEvent event = LoanApprovalEmailEvent.builder()
                .customerName(appUser.getName())
                .loanDueDate(loanRequest.getRepaymentDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .loanRepaymentAmount(loanRequest.getRepaymentAmount())
                .recipient(appUser.getEmail())
                .loanType(loanRequest.getLoanType().name())
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_REQUEST_APPROVED, new EventModel<>(event));
    }
}
