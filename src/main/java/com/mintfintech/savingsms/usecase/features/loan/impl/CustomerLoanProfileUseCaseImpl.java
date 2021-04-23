package com.mintfintech.savingsms.usecase.features.loan.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.CustomerLoanProfileEntityDao;
import com.mintfintech.savingsms.domain.dao.EmployeeInformationEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.ResourceFileEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CustomerLoanProfileEntity;
import com.mintfintech.savingsms.domain.entities.EmployeeInformationEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.ResourceFileEntity;
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.models.CustomerLoanProfileSearchDTO;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.AuditTrailService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.features.loan.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.ImageResourceUseCase;
import com.mintfintech.savingsms.usecase.data.request.CustomerProfileSearchRequest;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.models.EmploymentInformationModel;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;
import com.mintfintech.savingsms.utils.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final ImageResourceUseCase imageResourceUseCase;
    private final ResourceFileEntityDao resourceFileEntityDao;
    private final AuditTrailService auditTrailService;
    private final LoanRequestEntityDao loanRequestEntityDao;

    @Override
    @Transactional
    public LoanCustomerProfileModel createPaydayCustomerLoanProfile(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request) {

        validateEmploymentLetter(request.getEmploymentLetter());

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());

        CustomerLoanProfileEntity customerLoanProfileEntity;

        Optional<CustomerLoanProfileEntity> optionalCustomerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser);

        if (optionalCustomerLoanProfileEntity.isPresent()) {
            customerLoanProfileEntity = optionalCustomerLoanProfileEntity.get();

            if (customerLoanProfileEntity.getEmployeeInformation() != null) {
                throw new BadRequestException("An Employment Information already exists for this user.");
            }

            EmployeeInformationEntity employeeInformationEntity = createEmploymentInformation(request);
            customerLoanProfileEntity.setEmployeeInformation(employeeInformationEntity);
            customerLoanProfileEntityDao.saveRecord(customerLoanProfileEntity);

        } else {

            EmployeeInformationEntity employeeInformationEntity = createEmploymentInformation(request);

            CustomerLoanProfileEntity newCustomerLoanProfile = CustomerLoanProfileEntity.builder()
                    .appUser(appUser)
                    .employeeInformation(employeeInformationEntity)
                    .build();
            customerLoanProfileEntityDao.saveRecord(newCustomerLoanProfile);
        }

        return getLoanCustomerProfile(currentUser, "PAYDAY");
    }

    @Override
    public LoanCustomerProfileModel updateCustomerEmploymentInformation(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request) {

        if (request.getEmploymentLetter() != null){
            validateEmploymentLetter(request.getEmploymentLetter());
        }

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser)
                .orElseThrow(() -> new NotFoundException("No Loan Customer Profile Exists for this User"));

        if (customerLoanProfileEntity.getEmployeeInformation() == null) {
            throw new NotFoundException("An Employment Information does not exists for this user.");
        }

        EmployeeInformationEntity employeeInfo = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());

        updateEmploymentInformation(request, employeeInfo);

        LoanCustomerProfileModel loanCustomerProfileModel = toLoanCustomerProfileModel(customerLoanProfileEntity);
        loanCustomerProfileModel.setEmploymentInformation(addEmployeeInformationToCustomerLoanProfile(customerLoanProfileEntity));

        return loanCustomerProfileModel;
    }

    @Override
    public LoanCustomerProfileModel getLoanCustomerProfile(AuthenticatedUser currentUser, String loanType) {

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser).orElseThrow(
                () -> new BadRequestException("No Customer Loan Profile exists for this")
        );

        LoanCustomerProfileModel loanCustomerProfileModel = toLoanCustomerProfileModel(customerLoanProfileEntity);

        if (LoanTypeConstant.valueOf(loanType).equals(LoanTypeConstant.PAYDAY)) {
            loanCustomerProfileModel.setMaxLoanPercent(applicationProperty.getPayDayMaxLoanPercentAmount());
            loanCustomerProfileModel.setInterestRate(applicationProperty.getPayDayLoanInterestRate());
            loanCustomerProfileModel.setEmploymentInformation(addEmployeeInformationToCustomerLoanProfile(customerLoanProfileEntity));
            loanCustomerProfileModel.setHasActivePayDayLoan(loanRequestEntityDao.countActivePayDayLoan(appUser) > 0);
        }

        return loanCustomerProfileModel;
    }

    @Override
    public PagedDataResponse<LoanCustomerProfileModel> getPagedLoanCustomerProfiles(CustomerProfileSearchRequest searchRequest, int page, int size) {

        CustomerLoanProfileSearchDTO searchDTO = CustomerLoanProfileSearchDTO.builder()
                .fromDate(searchRequest.getFromDate() != null ? searchRequest.getFromDate().atStartOfDay() : null)
                .toDate(searchRequest.getToDate() != null ? searchRequest.getToDate().atTime(23, 59) : null)
                .verificationStatus(!searchRequest.getVerificationStatus().equals("ALL") ? ApprovalStatusConstant.valueOf(searchRequest.getVerificationStatus()) : null)
                .build();

        Page<CustomerLoanProfileEntity> loanProfileEntityPage = customerLoanProfileEntityDao.searchVerifiedCustomerProfile(searchDTO, page, size);

        return new PagedDataResponse<>(loanProfileEntityPage.getTotalElements(), loanProfileEntityPage.getTotalPages(),
                loanProfileEntityPage.get().map(this::toLoanCustomerProfileModel)
                        .collect(Collectors.toList()));
    }

    @Override
    public LoanCustomerProfileModel getCustomerEmployerInfo(long customerLoanProfileId) {

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findById(customerLoanProfileId).orElseThrow(
                () -> new BadRequestException("No Customer Loan Profile exists for this")
        );

        LoanCustomerProfileModel loanCustomerProfileModel = toLoanCustomerProfileModel(customerLoanProfileEntity);
        loanCustomerProfileModel.setEmploymentInformation(addEmployeeInformationToCustomerLoanProfile(customerLoanProfileEntity));
        return loanCustomerProfileModel;
    }

    @Override
    @Transactional
    public LoanCustomerProfileModel verifyEmploymentInformation(AuthenticatedUser currentUser, long customerLoanProfileId, boolean isVerified, String reason) {

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao
                .findById(customerLoanProfileId)
                .orElseThrow(() -> new BadRequestException("The Customer Loan Profile was not found for this id " + customerLoanProfileId));

        EmployeeInformationEntity employeeInformationEntity = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());

        EmployeeInformationEntity oldState = new EmployeeInformationEntity();
        BeanUtils.copyProperties(employeeInformationEntity, oldState);

        employeeInformationEntity.setVerificationStatus(isVerified ? ApprovalStatusConstant.APPROVED : ApprovalStatusConstant.REJECTED);
        employeeInformationEntity.setRejectionReason(isVerified ? null : reason);
        employeeInformationEntityDao.saveRecord(employeeInformationEntity);

        if (!isVerified){
            AppUserEntity appUserEntity = appUserEntityDao.getRecordById(customerLoanProfileEntity.getAppUser().getId());
            List<LoanRequestEntity> loans = loanRequestEntityDao.getLoansByAppUser(appUserEntity, LoanTypeConstant.PAYDAY.name());

            for (LoanRequestEntity loanRequestEntity : loans){
                if (loanRequestEntity.getApprovalStatus() == ApprovalStatusConstant.PENDING){
                    loanRequestEntity.setApprovalStatus(ApprovalStatusConstant.REJECTED);
                    loanRequestEntity.setRejectionReason("Customer Employment Profile was rejected");

                    loanRequestEntityDao.saveRecord(loanRequestEntity);
                }
            }
        }

        String description = String.format("Verified the Employment information for this loan customer: %s", oldState.getId());
        auditTrailService.createAuditLog(currentUser, AuditTrailService.AuditType.UPDATE, description, customerLoanProfileEntity, oldState);

        LoanCustomerProfileModel loanCustomerProfileModel = toLoanCustomerProfileModel(customerLoanProfileEntity);
        loanCustomerProfileModel.setEmploymentInformation(addEmployeeInformationToCustomerLoanProfile(customerLoanProfileEntity));
        return loanCustomerProfileModel;
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

        LoanCustomerProfileModel loanCustomerProfileModel = toLoanCustomerProfileModel(customerLoanProfileEntity);
        loanCustomerProfileModel.setEmploymentInformation(addEmployeeInformationToCustomerLoanProfile(customerLoanProfileEntity));
        return loanCustomerProfileModel;
    }

    @Override
    public void updateCustomerRating(AppUserEntity currentUser) {

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(currentUser)
                .orElseThrow(() -> new BadRequestException("No Loan Profile exist for this user"));

        double totalLoanCount = loanRequestEntityDao.countTotalLoans(currentUser);
        double totalRepaymentFailed = loanRequestEntityDao.countTotalLoansPastRepaymentDueDate(currentUser);

        double rating = ((totalLoanCount - totalRepaymentFailed)/totalLoanCount) * 5.0;

        customerLoanProfileEntity.setRating(rating);

        customerLoanProfileEntityDao.saveRecord(customerLoanProfileEntity);
    }

    @Override
    public EmploymentInformationModel getEmploymentInfo(AuthenticatedUser currentUser) {
        return null;
    }

    private void updateEmploymentInformation(EmploymentDetailCreationRequest request, EmployeeInformationEntity info){

        if (request.getEmploymentLetter() != null){
            ResourceFileEntity employmentLetter = imageResourceUseCase.createImage("employment-letters", request.getEmploymentLetter());
            info.setEmploymentLetter(employmentLetter);
        }

        info.setVerificationStatus(ApprovalStatusConstant.PENDING);
        info.setEmployerEmail(request.getEmployerEmail());
        info.setEmployerAddress(request.getEmployerAddress());
        info.setEmployerPhoneNo(request.getEmployerPhoneNo());
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

        return employeeInformationEntityDao.saveRecord(employeeInformationEntity);
    }

    private void validateEmploymentLetter(MultipartFile employmentLetter) {
        double sizeInMb = employmentLetter.getSize() * 1.0 / (1024 * 1024);
        if (sizeInMb > applicationProperty.getFileUploadMaximumSize()) {
            throw new BadRequestException("Maximum file size is " + applicationProperty.getFileUploadMaximumSize() + "MB.");
        }
    }

    @Override
    public LoanCustomerProfileModel toLoanCustomerProfileModel(CustomerLoanProfileEntity customerLoanProfileEntity) {

        AppUserEntity appUser = appUserEntityDao.getRecordById(customerLoanProfileEntity.getAppUser().getId());

        LoanCustomerProfileModel loanCustomerProfileModel = new LoanCustomerProfileModel();
        loanCustomerProfileModel.setCustomerName(appUser.getName());
        loanCustomerProfileModel.setEmail(appUser.getEmail());
        loanCustomerProfileModel.setBlacklistReason(customerLoanProfileEntity.getBlacklistReason());
        loanCustomerProfileModel.setBlacklistStatus(customerLoanProfileEntity.isBlacklisted());
        loanCustomerProfileModel.setId(customerLoanProfileEntity.getId());
        loanCustomerProfileModel.setPhoneNumber(appUser.getPhoneNumber());
        loanCustomerProfileModel.setRating(customerLoanProfileEntity.getRating());

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
            employmentInformationModel.setRejectionReason(employeeInformationEntity.getRejectionReason());
        }

        return employmentInformationModel;
    }
}
