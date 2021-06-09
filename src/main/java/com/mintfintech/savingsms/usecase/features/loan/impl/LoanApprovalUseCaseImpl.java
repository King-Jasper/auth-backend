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
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.features.loan.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.features.loan.LoanApprovalUseCase;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Transactional
    @Override
    public LoanModel approveLoanRequest(AuthenticatedUser authenticatedUser, String loanId, String reason, boolean approved) {

        LoanRequestEntity loanRequestEntity = loanRequestEntityDao.findByLoanId(loanId)
                .orElseThrow(() -> new BadRequestException("Loan request for this loanId " + loanId + " does not exist"));

        if (loanRequestEntity.getApprovalStatus() != ApprovalStatusConstant.PENDING) {
            throw new BadRequestException("This loan request is not in pending state");
        }
        AppUserEntity appUser = appUserEntityDao.getRecordById(loanRequestEntity.getRequestedBy().getId());

        if (approved) {

            processLoanApproval(loanRequestEntity, appUser, authenticatedUser);
        } else {

            processLoanDecline(loanRequestEntity, appUser, authenticatedUser, reason);
        }

        return getLoansUseCase.toLoanModel(loanRequestEntity);
    }

    @Override
    public void processApprovedLoans() {

        List<LoanRequestEntity> loans = loanRequestEntityDao.getApprovedLoans();

        for (LoanRequestEntity loan : loans) {
            MsClientResponse<NewLoanAccountResponseCBS> msClientResponse = coreBankingServiceClient.getLoanAccountDetails(loan.getTrackingReference());

            if (msClientResponse.isSuccess() && StringUtils.isNotEmpty(msClientResponse.getData().getAccountNumber())) {

                NewLoanAccountResponseCBS responseCBS = msClientResponse.getData();

                loan.setAccountNumber(responseCBS.getAccountNumber());
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
            }
        }
    }

    private void processLoanApproval(LoanRequestEntity loanRequest, AppUserEntity appUser, AuthenticatedUser authenticatedUser) {
        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser)
                .orElseThrow(() -> new NotFoundException("No Loan Customer Profile Exists for this User"));

        if (loanRequest.getLoanType() == LoanTypeConstant.PAYDAY) {
            EmployeeInformationEntity employeeInfo = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());
            if (employeeInfo.getVerificationStatus() != ApprovalStatusConstant.APPROVED) {
                throw new BadRequestException("Employment Information have not been verified for this user");
            }
        }

        MintBankAccountEntity bankAccount = mintBankAccountEntityDao.getRecordById(loanRequest.getBankAccount().getId());

        LoanApplicationRequestCBS request = LoanApplicationRequestCBS.builder()
                .loanType(loanRequest.getLoanType().name())
                .accountNumber(bankAccount.getAccountNumber())
                .amount(loanRequest.getLoanAmount().intValue())
                .build();

        MsClientResponse<LoanApplicationResponseCBS> msClientResponse = coreBankingServiceClient.createLoanApplication(request);

        if (msClientResponse.getStatusCode() == HttpStatus.OK.value()
                && msClientResponse.isSuccess()
                && StringUtils.isNotEmpty(msClientResponse.getData().getTrackingReference())
                && msClientResponse.getData().isSuccess()
        ) {
            LoanApplicationResponseCBS responseCBS = msClientResponse.getData();

            loanRequest.setApprovalStatus(ApprovalStatusConstant.APPROVED);
            loanRequest.setApprovedDate(LocalDateTime.now());
            loanRequest.setApproveByName(authenticatedUser.getUsername());
            loanRequest.setApproveByUserId(authenticatedUser.getUserId());
            loanRequest.setTrackingReference(responseCBS.getTrackingReference());
            loanRequest.setRepaymentDueDate(LocalDateTime.now().plusDays(applicationProperty.getPayDayLoanMaxTenor()));
            loanRequestEntityDao.saveRecord(loanRequest);

            MintAccountEntity mintAccount = mintAccountEntityDao.getRecordById(bankAccount.getMintAccount().getId());
            if (StringUtils.isEmpty(mintAccount.getBankOneCustomerId())) {
                mintAccount.setBankOneCustomerId(responseCBS.getCustomerId());
                mintAccountEntityDao.saveRecord(mintAccount);
            }
            sendLoanApprovalEmail(loanRequest, appUser);
        } else {
            String message = String.format("Loan Id: %s; message: %s", loanRequest.getLoanId(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Loan Creation Failure", "Loan Creation Failed", message);

            throw new BusinessLogicConflictException("Unable to approve loan at the moment. Please try again later.");
        }
    }

    private void processLoanDecline(LoanRequestEntity loanRequest,
                                    AppUserEntity appUser,
                                    AuthenticatedUser authenticatedUser,
                                    String reason
    ) {
        loanRequest.setApprovalStatus(ApprovalStatusConstant.DECLINED);
        loanRequest.setRepaymentStatus(LoanRepaymentStatusConstant.CANCELLED);
        loanRequest.setApproveByName(authenticatedUser.getUsername());
        loanRequest.setApproveByUserId(authenticatedUser.getUserId());
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
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_REQUEST_APPROVED, new EventModel<>(event));
    }
}
