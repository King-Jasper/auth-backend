package com.mintfintech.savingsms.usecase.features.loan.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.CustomerLoanProfileSearchDTO;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.LoanSearchDTO;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.AuditTrailService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.events.outgoing.EmploymentInfoUpdateEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.LoanDeclineEmailEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.LoanEmailEvent;
import com.mintfintech.savingsms.usecase.data.response.*;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.loan.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.ImageResourceUseCase;
import com.mintfintech.savingsms.usecase.data.request.CustomerProfileSearchRequest;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.models.CustomerLoanProfileDashboard;
import com.mintfintech.savingsms.usecase.models.EmploymentInformationModel;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;
import com.mintfintech.savingsms.utils.MintStringUtil;
import com.mintfintech.savingsms.utils.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerLoanProfileUseCaseImpl implements CustomerLoanProfileUseCase {

    private final ApplicationProperty applicationProperty;
    private final EmployeeInformationEntityDao employeeInformationEntityDao;
    private final CustomerLoanProfileEntityDao customerLoanProfileEntityDao;
    private final AppUserEntityDao appUserEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final ImageResourceUseCase imageResourceUseCase;
    private final ResourceFileEntityDao resourceFileEntityDao;
    private final AuditTrailService auditTrailService;
    private final LoanRequestEntityDao loanRequestEntityDao;
    private final ApplicationEventService applicationEventService;
    private final LoanReviewLogEntityDao loanReviewLogEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final SettingsEntityDao settingsEntityDao;
    private final CorporateUserEntityDao corporateUserEntityDao;
    private final HNILoanCustomerEntityDao hniLoanCustomerEntityDao;

    @Override
    @Transactional
    public LoanCustomerProfileModel createPaydayCustomerLoanProfile(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request) {

        validateEmploymentLetter(request.getEmploymentLetter());

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());

        CustomerLoanProfileEntity customerLoanProfileEntity;

        Optional<CustomerLoanProfileEntity> optionalCustomerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser);
        EmployeeInformationEntity employeeInformationEntity;

        if (optionalCustomerLoanProfileEntity.isPresent()) {
            customerLoanProfileEntity = optionalCustomerLoanProfileEntity.get();

            if (customerLoanProfileEntity.getEmployeeInformation() != null) {
                throw new BadRequestException("An Employment Information already exists for this user.");
            }

            employeeInformationEntity = createEmploymentInformation(request);
            customerLoanProfileEntity.setEmployeeInformation(employeeInformationEntity);
        } else {
            employeeInformationEntity = createEmploymentInformation(request);

            customerLoanProfileEntity = CustomerLoanProfileEntity.builder()
                    .appUser(appUser)
                    .employeeInformation(employeeInformationEntity)
                    .build();
        }
        customerLoanProfileEntityDao.saveRecord(customerLoanProfileEntity);

        publishEmploymentDetails(appUser, employeeInformationEntity);

        LoanEmailEvent loanEmailEvent = LoanEmailEvent.builder()
                .customerName(appUser.getName())
                .recipient(appUser.getEmail())
                .build();

        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_PROFILE_CREATION, new EventModel<>(loanEmailEvent));

        return getLoanCustomerProfile(appUser, accountEntity, customerLoanProfileEntity, "PAYDAY");
    }

    @Override
    public LoanDashboardResponse getLoanDashboardInformation(AuthenticatedUser authenticatedUser) {

        AppUserEntity currentUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        long count = 0;
        boolean accessBusinessLoan = false;
        boolean chequeRequired = false;

        String rateString = settingsEntityDao.getSettings(SettingsNameTypeConstant.BUSINESS_LOAN_RATE, "4.0");
        double businessRate = Double.parseDouble(rateString);

        if(applicationProperty.isLiveEnvironment()) {
            count = loanRequestEntityDao.countActiveLoan(currentUser, LoanTypeConstant.BUSINESS);
          //  accessBusinessLoan = MintStringUtil.enableBusinessLoanFeature(authenticatedUser.getAccountId());
           String accounts = settingsEntityDao.getSettings(SettingsNameTypeConstant.BUSINESS_LOAN_ACCESS_ACCOUNT_IDS, "");
           if(StringUtils.isNotEmpty(accounts)) {
             accessBusinessLoan = Arrays.stream(accounts.split(":")).anyMatch(data -> data.equalsIgnoreCase(authenticatedUser.getAccountId()));
           }
           if(!accessBusinessLoan) {
               Optional<HNILoanCustomerEntity> hniLoanCustomerOpt = hniLoanCustomerEntityDao.findRecord(mintAccount);
               if(hniLoanCustomerOpt.isPresent()) {
                   HNILoanCustomerEntity hniLoanCustomer = hniLoanCustomerOpt.get();
                   chequeRequired = hniLoanCustomer.isChequeRequired();
                   businessRate = hniLoanCustomer.getInterestRate();
               }
           }

        }

        LoanDashboardResponse response = new LoanDashboardResponse();
        response.setCanRequestBusinessLoan(count == 0);
        response.setChequeUploadRequired(chequeRequired);
        response.setBusinessLoanAvailable(accessBusinessLoan);
        response.setBusinessLoanMonthlyInterest(businessRate);
        response.setPaydayLoanAvailable(true);
        response.setMaximumDaysForReview(5);
        response.setMinimumDaysForReview(2);
        response.setPayDayLoanInterest(applicationProperty.getPayDayLoanInterestRate());
        List<LoanDuration> durations = new ArrayList<>();
        durations.add(new LoanDuration(1, "1 Month"));
        durations.add(new LoanDuration(2, "2 Months"));
        durations.add(new LoanDuration(3, "3 Months"));
        durations.add(new LoanDuration(4, "4 Months"));
        durations.add(new LoanDuration(5, "5 Months"));
        durations.add(new LoanDuration(6, "6 Months"));
        durations.add(new LoanDuration(9, "9 Months"));
        durations.add(new LoanDuration(12, "12 Months"));
        response.setBusinessLoanDurations(durations);
        return response;
    }

    @Override
    public LoanCustomerProfileModel updateCustomerEmploymentInformation(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request) {

        if (request.getEmploymentLetter() != null) {
            validateEmploymentLetter(request.getEmploymentLetter());
        }

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser)
                .orElseThrow(() -> new NotFoundException("No Loan Customer Profile Exists for this User"));

        if (customerLoanProfileEntity.getEmployeeInformation() == null) {
            throw new NotFoundException("An Employment Information does not exists for this user.");
        }

        EmployeeInformationEntity employeeInfo = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());

        updateEmploymentInformation(request, employeeInfo);

        publishEmploymentDetails(appUser, employeeInfo);

        LoanEmailEvent loanEmailEvent = LoanEmailEvent.builder()
                .recipient(applicationProperty.getLoanAdminEmail())
                .customerName(appUser.getName())
                .build();

        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_PROFILE_UPDATE_ADMIN, new EventModel<>(loanEmailEvent));

        LoanCustomerProfileModel loanCustomerProfileModel = toLoanCustomerProfileModel(mintAccount, customerLoanProfileEntity);
        loanCustomerProfileModel.setEmploymentInformation(addEmployeeInformationToCustomerLoanProfile(customerLoanProfileEntity));

        return loanCustomerProfileModel;
    }

    private LoanCustomerProfileModel getLoanCustomerProfile(AppUserEntity appUser, MintAccountEntity accountEntity, CustomerLoanProfileEntity customerLoanProfile, String loanType) {

        LoanCustomerProfileModel loanCustomerProfileModel = toLoanCustomerProfileModel(accountEntity, customerLoanProfile);

        if (LoanTypeConstant.valueOf(loanType).equals(LoanTypeConstant.PAYDAY)) {
            loanCustomerProfileModel.setMaxLoanPercent(applicationProperty.getPayDayMaxLoanPercentAmount());
            loanCustomerProfileModel.setInterestRate(applicationProperty.getPayDayLoanInterestRate());
            loanCustomerProfileModel.setEmploymentInformation(addEmployeeInformationToCustomerLoanProfile(customerLoanProfile));
            loanCustomerProfileModel.setHasActivePayDayLoan(loanRequestEntityDao.countActiveLoan(appUser, LoanTypeConstant.PAYDAY) > 0);
        }
        return loanCustomerProfileModel;
    }

    @Override
    public CustomerLoanProfileDashboard getLoanCustomerProfileDashboard(AuthenticatedUser currentUser, String loanType) {

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());
        MintAccountEntity mintAccountEntity = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());

        CustomerLoanProfileDashboard profileDashboard = new CustomerLoanProfileDashboard();
        profileDashboard.setHasCustomerProfile(true);

        Optional<CustomerLoanProfileEntity> optionalCustomerLoanProfile = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser);

        if (!optionalCustomerLoanProfile.isPresent()) {
            profileDashboard.setHasCustomerProfile(false);
            return profileDashboard;
        }

        LoanCustomerProfileModel loanCustomerProfile = getLoanCustomerProfile(appUser, mintAccountEntity, optionalCustomerLoanProfile.get(), loanType);
        profileDashboard.setCustomerProfile(loanCustomerProfile);

        return profileDashboard;
    }

    @Override
    public PagedDataResponse<LoanCustomerProfileModel> getPagedLoanCustomerProfiles(CustomerProfileSearchRequest searchRequest, int page, int size) {

        CustomerLoanProfileSearchDTO searchDTO = CustomerLoanProfileSearchDTO.builder()
                .fromDate(searchRequest.getFromDate() != null ? searchRequest.getFromDate().atStartOfDay() : null)
                .toDate(searchRequest.getToDate() != null ? searchRequest.getToDate().atTime(23, 59) : null)
                .verificationStatus(!searchRequest.getVerificationStatus().equals("ALL") ? ApprovalStatusConstant.valueOf(searchRequest.getVerificationStatus()) : null)
                .reviewStage(StringUtils.isNotEmpty(searchRequest.getReviewStage()) ? LoanReviewStageConstant.valueOf(searchRequest.getReviewStage()): null)
                .customerName(searchRequest.getCustomerName())
                .customerPhone(searchRequest.getCustomerPhone())
                .build();
        System.out.println(searchDTO.toString());
        Page<CustomerLoanProfileEntity> loanProfileEntityPage = customerLoanProfileEntityDao.searchVerifiedCustomerProfile(searchDTO, page, size);

        return new PagedDataResponse<>(loanProfileEntityPage.getTotalElements(), loanProfileEntityPage.getTotalPages(),
                loanProfileEntityPage.get().map(data -> toLoanCustomerProfileModel(null, data))
                        .collect(Collectors.toList()));
    }

    @Override
    public LoanCustomerProfileModel getCustomerEmployerInfo(long customerLoanProfileId) {
        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findById(customerLoanProfileId).orElseThrow(
                () -> new BadRequestException("No Customer Loan Profile exists for this")
        );
        LoanCustomerProfileModel loanCustomerProfileModel = toLoanCustomerProfileModel(null, customerLoanProfileEntity);
        loanCustomerProfileModel.setEmploymentInformation(addEmployeeInformationToCustomerLoanProfile(customerLoanProfileEntity));
        return loanCustomerProfileModel;
    }

    @Override
    @Transactional
    public LoanCustomerProfileModel verifyEmploymentInformation(AuthenticatedUser currentUser, long customerLoanProfileId, boolean isVerified, String reason) {

        LoanManager loanManager = LoanManager.getManager(currentUser);
        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao
                .findById(customerLoanProfileId)
                .orElseThrow(() -> new BadRequestException("The Customer Loan Profile was not found for this id " + customerLoanProfileId));

        EmployeeInformationEntity employeeInformationEntity = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());
        LoanReviewStageConstant reviewStage = employeeInformationEntity.getReviewStage();
        if(reviewStage == null || reviewStage == LoanReviewStageConstant.FIRST_REVIEW) {
           handleVerificationByLoanOfficer(loanManager, employeeInformationEntity, customerLoanProfileEntity, isVerified, reason);
        }else if(reviewStage == LoanReviewStageConstant.SECOND_REVIEW) {
            handleVerificationByRiskManager(loanManager, employeeInformationEntity, customerLoanProfileEntity, isVerified, reason);
        }
        LoanCustomerProfileModel loanCustomerProfileModel = toLoanCustomerProfileModel(null, customerLoanProfileEntity);
        loanCustomerProfileModel.setEmploymentInformation(addEmployeeInformationToCustomerLoanProfile(customerLoanProfileEntity));
        return loanCustomerProfileModel;
    }

    private void handleVerificationByRiskManager(LoanManager loanManager, EmployeeInformationEntity employeeInformationEntity, CustomerLoanProfileEntity customerLoanProfileEntity, boolean isVerified, String reason) {
        if(!loanManager.isRiskOfficer()) {
            throw new BusinessLogicConflictException("Sorry, review request can only be accepted by a risk management officer.");
        }
        AppUserEntity appUserEntity = appUserEntityDao.getRecordById(customerLoanProfileEntity.getAppUser().getId());
        String description;
        if(!isVerified) {
            employeeInformationEntity.setVerificationStatus(ApprovalStatusConstant.DECLINED);
            employeeInformationEntity.setDateRejected(LocalDateTime.now());
            employeeInformationEntity.setRejectionReason(reason);
            employeeInformationEntityDao.saveRecord(employeeInformationEntity);

            List<LoanRequestEntity> loans = loanRequestEntityDao.getLoansByAppUser(appUserEntity, LoanTypeConstant.PAYDAY.name());
            for (LoanRequestEntity loanRequestEntity : loans) {
                if (loanRequestEntity.getApprovalStatus() == ApprovalStatusConstant.PENDING) {
                    loanRequestEntity.setApprovalStatus(ApprovalStatusConstant.DECLINED);
                    loanRequestEntity.setDateRejected(LocalDateTime.now());
                    loanRequestEntity.setActiveLoan(false);
                    loanRequestEntity.setRejectionReason("Customer Employment Profile was rejected");
                    loanRequestEntityDao.saveRecord(loanRequestEntity);
                }
            }
            LoanDeclineEmailEvent event = LoanDeclineEmailEvent.builder()
                    .customerName(appUserEntity.getName())
                    .recipient(appUserEntity.getEmail())
                    .reason(reason)
                    .build();
            applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_PROFILE_DECLINED, new EventModel<>(event));
            description = "Declined with reason - "+reason;
        }else {
            employeeInformationEntity.setVerificationStatus(ApprovalStatusConstant.APPROVED);
            employeeInformationEntityDao.saveRecord(employeeInformationEntity);

            LoanEmailEvent loanEmailEvent = LoanEmailEvent.builder()
                    .customerName(appUserEntity.getName())
                    .recipient(appUserEntity.getEmail())
                    .build();
            applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_PROFILE_APPROVED, new EventModel<>(loanEmailEvent));
            description = "Detail final verification.";
        }
        LoanReviewLogEntity reviewLogEntity = LoanReviewLogEntity.builder()
                .reviewLogType(LoanReviewLogType.EMPLOYMENT_INFORMATION)
                .entityId(employeeInformationEntity.getId())
                .description(description)
                .reviewerName(loanManager.getReviewerName())
                .build();
        loanReviewLogEntityDao.saveRecord(reviewLogEntity);
    }

    private void handleVerificationByLoanOfficer(LoanManager loanManager, EmployeeInformationEntity employeeInformationEntity, CustomerLoanProfileEntity customerLoanProfileEntity, boolean isVerified, String reason) {
        if(!loanManager.isRiskOfficer()) {
            throw new BusinessLogicConflictException("Sorry, review request can only be accepted by a risk management officer.");
        }
        String description;
        if(!isVerified) {
            employeeInformationEntity.setVerificationStatus(ApprovalStatusConstant.DECLINED);
            employeeInformationEntity.setDateRejected(LocalDateTime.now());
            employeeInformationEntity.setRejectionReason(reason);
            employeeInformationEntityDao.saveRecord(employeeInformationEntity);

            AppUserEntity appUserEntity = appUserEntityDao.getRecordById(customerLoanProfileEntity.getAppUser().getId());
            List<LoanRequestEntity> loans = loanRequestEntityDao.getLoansByAppUser(appUserEntity, LoanTypeConstant.PAYDAY.name());
            for (LoanRequestEntity loanRequestEntity : loans) {
                if (loanRequestEntity.getApprovalStatus() == ApprovalStatusConstant.PENDING) {
                    loanRequestEntity.setApprovalStatus(ApprovalStatusConstant.DECLINED);
                    loanRequestEntity.setActiveLoan(false);
                    loanRequestEntity.setDateRejected(LocalDateTime.now());
                    loanRequestEntity.setRejectionReason("Customer Employment Profile was rejected");
                    loanRequestEntityDao.saveRecord(loanRequestEntity);
                }
            }
            LoanDeclineEmailEvent event = LoanDeclineEmailEvent.builder()
                    .customerName(appUserEntity.getName())
                    .recipient(appUserEntity.getEmail())
                    .reason(reason)
                    .build();
            applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_PROFILE_DECLINED, new EventModel<>(event));
            description = "Declined with reason - "+reason;
        }else {
            employeeInformationEntity.setReviewStage(LoanReviewStageConstant.SECOND_REVIEW);
            employeeInformationEntityDao.saveRecord(employeeInformationEntity);
            description = "Detail verified and moved to next review stage";
        }

        LoanReviewLogEntity reviewLogEntity = LoanReviewLogEntity.builder()
                .reviewLogType(LoanReviewLogType.EMPLOYMENT_INFORMATION)
                .entityId(employeeInformationEntity.getId())
                .description(description)
                .reviewerName(loanManager.getReviewerName())
                .build();
        loanReviewLogEntityDao.saveRecord(reviewLogEntity);
    }


    @Override
    public LoanCustomerProfileModel blackListCustomer(AuthenticatedUser currentUser, long customerLoanProfileId, boolean blacklist, String reason) {

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao
                .findById(customerLoanProfileId)
                .orElseThrow(() -> new BadRequestException("The Customer Loan Profile was not found for this id " + customerLoanProfileId));

        CustomerLoanProfileEntity oldState = new CustomerLoanProfileEntity();
        BeanUtils.copyProperties(customerLoanProfileEntity, oldState);

        customerLoanProfileEntity.setBlacklisted(blacklist);
        customerLoanProfileEntity.setBlacklistReason(blacklist ? reason : null);

        customerLoanProfileEntityDao.saveRecord(customerLoanProfileEntity);

        String description = String.format("Blacklisted this customer loan profile: %s", oldState.getId());
        auditTrailService.createAuditLog(currentUser, AuditTrailService.AuditType.UPDATE, description, customerLoanProfileEntity, oldState);

        LoanCustomerProfileModel loanCustomerProfileModel = toLoanCustomerProfileModel(null, customerLoanProfileEntity);
        loanCustomerProfileModel.setEmploymentInformation(addEmployeeInformationToCustomerLoanProfile(customerLoanProfileEntity));
        return loanCustomerProfileModel;
    }

    @Override
    public void updateCustomerRating(AppUserEntity currentUser) {

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(currentUser)
                .orElseThrow(() -> new BadRequestException("No Loan Profile exist for this user"));

        double totalLoanCount = loanRequestEntityDao.countTotalLoans(currentUser);
        double totalRepaymentFailed = loanRequestEntityDao.countTotalLoansPastRepaymentDueDate(currentUser);

        double rating = ((totalLoanCount - totalRepaymentFailed) / totalLoanCount) * 5.0;

        customerLoanProfileEntity.setRating(rating);

        customerLoanProfileEntityDao.saveRecord(customerLoanProfileEntity);
    }

    @Override
    public EmploymentInformationModel getEmploymentInfo(AuthenticatedUser currentUser) {

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());

        CustomerLoanProfileEntity customerLoanProfile = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser).orElseThrow(
                () -> new BadRequestException("No Customer Loan Profile exists for this")
        );
        return addEmployeeInformationToCustomerLoanProfile(customerLoanProfile);
    }

    private void updateEmploymentInformation(EmploymentDetailCreationRequest request, EmployeeInformationEntity info) {

        if (request.getEmploymentLetter() != null) {
            ResourceFileEntity employmentLetter = imageResourceUseCase.createImage("employment-letters", request.getEmploymentLetter());
            info.setEmploymentLetter(employmentLetter);
        }

        info.setVerificationStatus(ApprovalStatusConstant.PENDING);
        info.setEmployerEmail(request.getEmployerEmail());
        info.setEmployerAddress(request.getEmployerAddress());
        info.setEmployerPhoneNo(PhoneNumberUtils.toInternationalFormat(request.getEmployerPhoneNo()));
        info.setMonthlyIncome(request.getMonthlyIncome());
        info.setOrganizationName(request.getOrganizationName());
        info.setWorkEmail(request.getWorkEmail());
        info.setOrganizationUrl(request.getOrganizationUrl());

        employeeInformationEntityDao.saveRecord(info);
    }

    private EmployeeInformationEntity createEmploymentInformation(EmploymentDetailCreationRequest request) {
        ResourceFileEntity employmentLetter = imageResourceUseCase.createImage("employment-letters", request.getEmploymentLetter());

        EmployeeInformationEntity employeeInformationEntity = EmployeeInformationEntity.builder()
                .employerAddress(request.getEmployerAddress())
                .employerEmail(request.getEmployerEmail())
                .employerPhoneNo(PhoneNumberUtils.toInternationalFormat(request.getEmployerPhoneNo()))
                .employmentLetter(employmentLetter)
                .monthlyIncome(request.getMonthlyIncome())
                .organizationName(request.getOrganizationName())
                .organizationUrl(request.getOrganizationUrl())
                .workEmail(request.getWorkEmail())
                .build();

        EmployeeInformationEntity informationEntity = employeeInformationEntityDao.saveRecord(employeeInformationEntity);
        return informationEntity;
    }

    private void publishEmploymentDetails(AppUserEntity appUserEntity, EmployeeInformationEntity informationEntity) {
        ResourceFileEntity employmentLetter = informationEntity.getEmploymentLetter();
        if(!Hibernate.isInitialized(employmentLetter)) {
            employmentLetter = resourceFileEntityDao.getRecordById(employmentLetter.getId());
        }
        EmploymentInfoUpdateEvent infoUpdateEvent = EmploymentInfoUpdateEvent.builder()
                .employerAddress(informationEntity.getEmployerAddress())
                .employerEmail(informationEntity.getEmployerEmail())
                .employerPhoneNumber(informationEntity.getEmployerPhoneNo())
                .monthlySalary(informationEntity.getMonthlyIncome())
                .organizationName(informationEntity.getOrganizationName())
                .organizationUrl(informationEntity.getOrganizationUrl())
                .userId(appUserEntity.getUserId())
                .workEmail(informationEntity.getWorkEmail())
                .employmentLetterFileId(employmentLetter.getFileId())
                .employmentLetterFileName(employmentLetter.getFileName())
                .employmentLetterFileSize(employmentLetter.getFileSizeInKB())
                .employmentLetterFileUrl(employmentLetter.getUrl())
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.EMPLOYMENT_INFORMATION_UPDATE, new EventModel<>(infoUpdateEvent));
    }

    private void validateEmploymentLetter(MultipartFile employmentLetter) {
       // System.out.println("original file name - "+employmentLetter.getOriginalFilename());
       // System.out.println("file name - "+employmentLetter.getName());
        double sizeInMb = employmentLetter.getSize() * 1.0 / (1024 * 1024);
        if (sizeInMb > applicationProperty.getFileUploadMaximumSize()) {
            throw new BadRequestException("Maximum file size is " + applicationProperty.getFileUploadMaximumSize() + "MB.");
        }
    }

    @Override
    public LoanCustomerProfileModel toLoanCustomerProfileModel(MintAccountEntity mintAccount, CustomerLoanProfileEntity customerLoanProfileEntity) {
        AppUserEntity appUser = appUserEntityDao.getRecordById(customerLoanProfileEntity.getAppUser().getId());
        if(mintAccount == null && appUser.getPrimaryAccount() != null) {
            if(appUser.getPrimaryAccount() == null) {
                Optional<CorporateUserEntity> opt = corporateUserEntityDao.findTopByAppUser(appUser);
                if(!opt.isPresent()) {
                    throw new BusinessLogicConflictException("Sorry, this service is not available to your business type.");
                }
                mintAccount = opt.get().getCorporateAccount();
            }else {
                mintAccount = mintAccountEntityDao.getRecordById(appUser.getPrimaryAccount().getId());
            }
        }
        MintBankAccountEntity mintBankAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(mintAccount, BankAccountTypeConstant.CURRENT);
        TierLevelEntity tierLevelEntity = mintBankAccount.getAccountTierLevel();
        LoanCustomerProfileModel loanCustomerProfileModel = new LoanCustomerProfileModel();
        loanCustomerProfileModel.setCustomerName(appUser.getName());
        loanCustomerProfileModel.setEmail(appUser.getEmail());
        loanCustomerProfileModel.setBlacklistReason(customerLoanProfileEntity.getBlacklistReason());
        loanCustomerProfileModel.setBlacklistStatus(customerLoanProfileEntity.isBlacklisted());
        loanCustomerProfileModel.setId(customerLoanProfileEntity.getId());
        loanCustomerProfileModel.setPhoneNumber(appUser.getPhoneNumber());
        loanCustomerProfileModel.setRating(customerLoanProfileEntity.getRating());
        loanCustomerProfileModel.setAccountNumber(mintBankAccount.getAccountNumber());
        loanCustomerProfileModel.setAccountTier(tierLevelEntity.getLevel().name());
        loanCustomerProfileModel.setEmploymentInformation(addEmployeeInformationToCustomerLoanProfile(customerLoanProfileEntity));
        return loanCustomerProfileModel;
    }

    @Override
    public LoanCustomerProfileModel getLoanProfileForBusinessLoan(LoanRequestEntity loanRequestEntity) {
        AppUserEntity appUser = appUserEntityDao.getRecordById(loanRequestEntity.getRequestedBy().getId());
        MintBankAccountEntity mintAccount = mintBankAccountEntityDao.getRecordById(loanRequestEntity.getBankAccount().getId());
        TierLevelEntity tierLevelEntity = mintAccount.getAccountTierLevel();
        LoanCustomerProfileModel loanCustomerProfileModel = new LoanCustomerProfileModel();
        loanCustomerProfileModel.setCustomerName(appUser.getName());
        loanCustomerProfileModel.setEmail(appUser.getEmail());
        loanCustomerProfileModel.setBlacklistReason("");
        loanCustomerProfileModel.setBlacklistStatus(false);
        loanCustomerProfileModel.setId(loanRequestEntity.getId());
        loanCustomerProfileModel.setPhoneNumber(appUser.getPhoneNumber());
        loanCustomerProfileModel.setRating(5.0);
        loanCustomerProfileModel.setAccountNumber(mintAccount.getAccountNumber());
        loanCustomerProfileModel.setAccountTier(tierLevelEntity.getLevel().name());
       // loanCustomerProfileModel.setEmploymentInformation(addEmployeeInformationToCustomerLoanProfile(customerLoanProfileEntity));
        return loanCustomerProfileModel;
    }

    private EmploymentInformationModel addEmployeeInformationToCustomerLoanProfile(CustomerLoanProfileEntity customerLoanProfileEntity) {

        EmploymentInformationModel employmentInformationModel = null;

        if (customerLoanProfileEntity.getEmployeeInformation() != null) {
            EmployeeInformationEntity employeeInformationEntity = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());
            employmentInformationModel = new EmploymentInformationModel();

            ResourceFileEntity resourceFileEntity = resourceFileEntityDao.getRecordById(employeeInformationEntity.getEmploymentLetter().getId());

            employmentInformationModel.setEmployerEmail(employeeInformationEntity.getEmployerEmail());
            employmentInformationModel.setEmploymentLetterUrl(resourceFileEntity.getUrl());
            employmentInformationModel.setEmployerAddress(employeeInformationEntity.getEmployerAddress());
            employmentInformationModel.setEmployerPhoneNo(PhoneNumberUtils.toNationalFormat(employeeInformationEntity.getEmployerPhoneNo()));
            employmentInformationModel.setMonthlyIncome(employeeInformationEntity.getMonthlyIncome().doubleValue());
            employmentInformationModel.setVerified(employeeInformationEntity.getVerificationStatus().name());
            employmentInformationModel.setOrganizationName(employeeInformationEntity.getOrganizationName());
            employmentInformationModel.setOrganizationUrl(employeeInformationEntity.getOrganizationUrl());
            employmentInformationModel.setWorkEmail(employeeInformationEntity.getWorkEmail());
            employmentInformationModel.setRejectionReason(StringUtils.defaultString(employeeInformationEntity.getRejectionReason()));
            if(employeeInformationEntity.getVerificationStatus() == ApprovalStatusConstant.REJECTED || employeeInformationEntity.getVerificationStatus() == ApprovalStatusConstant.DECLINED){
                 employmentInformationModel.setDateRejected(employeeInformationEntity.getDateRejected() != null ?
                         employeeInformationEntity.getDateRejected().format(DateTimeFormatter.ISO_DATE_TIME) : employeeInformationEntity.getDateModified().format(DateTimeFormatter.ISO_DATE_TIME));
            }
        }
        return employmentInformationModel;
    }

}
