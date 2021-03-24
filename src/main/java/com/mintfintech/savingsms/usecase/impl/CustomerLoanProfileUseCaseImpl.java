package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.CustomerLoanProfileEntityDao;
import com.mintfintech.savingsms.domain.dao.EmployeeInformationEntityDao;
import com.mintfintech.savingsms.domain.dao.ResourceFileEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CustomerLoanProfileEntity;
import com.mintfintech.savingsms.domain.entities.EmployeeInformationEntity;
import com.mintfintech.savingsms.domain.entities.ResourceFileEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.AuditTrailService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.ImageResourceUseCase;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.models.EmploymentInformationModel;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;
import com.mintfintech.savingsms.utils.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
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

    @Override
    @Transactional
    public LoanCustomerProfileModel addCustomerEmploymentInformation(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request) {

        validateEmploymentLetter(request.getEmploymentLetter());

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());

        CustomerLoanProfileEntity customerLoanProfileEntity;

        Optional<CustomerLoanProfileEntity> optionalCustomerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser);

        if (optionalCustomerLoanProfileEntity.isPresent()) {
            customerLoanProfileEntity = optionalCustomerLoanProfileEntity.get();

            if (customerLoanProfileEntity.getEmployeeInformation() != null) {
                throw new BadRequestException("An Employee Information already exists for this user.");
            }

            EmployeeInformationEntity employeeInformationEntity = createEmploymentInformation(request);
            customerLoanProfileEntity.setEmployeeInformation(employeeInformationEntity);
            customerLoanProfileEntity = customerLoanProfileEntityDao.saveRecord(customerLoanProfileEntity);

        } else {

            EmployeeInformationEntity employeeInformationEntity = createEmploymentInformation(request);

            CustomerLoanProfileEntity newCustomerLoanProfile = CustomerLoanProfileEntity.builder()
                    .user(appUser)
                    .employeeInformation(employeeInformationEntity)
                    .build();
            customerLoanProfileEntity = customerLoanProfileEntityDao.saveRecord(newCustomerLoanProfile);
        }

        return buildResponse(customerLoanProfileEntity);
    }

    @Override
    public LoanCustomerProfileModel verifyEmploymentInformation(AuthenticatedUser currentUser, long customerLoanProfileId) {

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao
                .findById(customerLoanProfileId)
                .orElseThrow(() -> new BadRequestException("The Customer Loan Profile was not found for this id " + customerLoanProfileId));

        if (customerLoanProfileEntity.getEmployeeInformation() == null) {
            throw new BadRequestException("No Employee Information exist for this customer loan profile id " + customerLoanProfileId);
        }

        CustomerLoanProfileEntity oldState = new CustomerLoanProfileEntity();
        BeanUtils.copyProperties(customerLoanProfileEntity, oldState);

        EmployeeInformationEntity employeeInformationEntity = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());
        employeeInformationEntity.setVerified(true);

        employeeInformationEntityDao.saveRecord(employeeInformationEntity);

        String description = String.format("Verified the Employment information for this loan customer: %s", oldState.getId());
        auditTrailService.createAuditLog(currentUser, AuditTrailService.AuditType.UPDATE, description, customerLoanProfileEntity, oldState);

        return buildResponse(customerLoanProfileEntity);
    }

    @Override
    public LoanCustomerProfileModel blackListCustomer(AuthenticatedUser currentUser, long customerLoanProfileId, String reason) {

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao
                .findById(customerLoanProfileId)
                .orElseThrow(() -> new BadRequestException("The Customer Loan Profile was not found for this id " + customerLoanProfileId));

        CustomerLoanProfileEntity oldState = new CustomerLoanProfileEntity();
        BeanUtils.copyProperties(customerLoanProfileEntity, oldState);

        customerLoanProfileEntity.setBlacklisted(true);
        customerLoanProfileEntity.setBlacklistReason(reason);

        customerLoanProfileEntityDao.saveRecord(customerLoanProfileEntity);

        String description = String.format("Blacklisted this customer loan profile: %s", oldState.getId());
        auditTrailService.createAuditLog(currentUser, AuditTrailService.AuditType.UPDATE, description, customerLoanProfileEntity, oldState);

        return buildResponse(customerLoanProfileEntity);
    }

    @Override
    public BigDecimal getLoanMaxAmount(AuthenticatedUser currentUser, String loanType) {

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser)
                .orElseThrow(() -> new BadRequestException("No Loan Profile exist for this user"));

        LoanTypeConstant loanTypeConstant = LoanTypeConstant.valueOf(loanType);

        BigDecimal maxAmount = BigDecimal.ZERO;

        if (loanTypeConstant.equals(LoanTypeConstant.PAYDAY)) {

            if (customerLoanProfileEntity.getEmployeeInformation() == null) {
                throw new BadRequestException("An Employee Information does not exist for this user.");
            }

            EmployeeInformationEntity employeeInformationEntity = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());

            maxAmount = employeeInformationEntity.getMonthlyIncome().multiply(BigDecimal.valueOf(applicationProperty.getPayDayMaxLoanPercentAmount() / 100.0));

        }

        return maxAmount;
    }

    @Override
    public List<LoanCustomerProfileModel> getLoanCustomerProfiles(boolean blacklisted, boolean employeeInfoVerified) {
        List<CustomerLoanProfileEntity> profileEntities = customerLoanProfileEntityDao.getBlackListedAndVerifiedCustomers(blacklisted, employeeInfoVerified);

        return profileEntities.stream().map(this::buildResponse).collect(Collectors.toList());
    }

    private EmployeeInformationEntity createEmploymentInformation(EmploymentDetailCreationRequest request) {
        ResourceFileEntity employmentLetter = imageResourceUseCase.createImage("employment-letters", request.getEmploymentLetter());

        EmployeeInformationEntity employeeInformationEntity = EmployeeInformationEntity.builder()
                .employerAddress(request.getEmployerAddress())
                .employerEmail(request.getEmployerEmail())
                .employerPhoneNo(PhoneNumberUtil.toInternationalFormat(request.getEmployerPhoneNo()))
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

    private LoanCustomerProfileModel buildResponse(CustomerLoanProfileEntity customerLoanProfileEntity) {

        LoanCustomerProfileModel loanCustomerProfileModel = new LoanCustomerProfileModel();
        loanCustomerProfileModel.setBlacklisted(customerLoanProfileEntity.isBlacklisted());
        loanCustomerProfileModel.setBlacklistReason(customerLoanProfileEntity.getBlacklistReason());
        loanCustomerProfileModel.setId(customerLoanProfileEntity.getId());
        loanCustomerProfileModel.setRating(customerLoanProfileEntity.getRating());

        if (customerLoanProfileEntity.getEmployeeInformation() != null) {
            EmployeeInformationEntity employeeInformationEntity = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());
            EmploymentInformationModel employmentInformationModel = new EmploymentInformationModel();

            ResourceFileEntity resourceFileEntity = resourceFileEntityDao.getRecordById(employeeInformationEntity.getEmploymentLetter().getId());

            employmentInformationModel.setEmployerEmail(employeeInformationEntity.getEmployerEmail());
            employmentInformationModel.setEmploymentLetterUrl(resourceFileEntity.getUrl());
            employmentInformationModel.setEmployerAddress(employeeInformationEntity.getEmployerAddress());
            employmentInformationModel.setEmployerPhoneNo(PhoneNumberUtil.toNationalFormat(employeeInformationEntity.getEmployerPhoneNo()));
            employmentInformationModel.setMonthlyIncome(employeeInformationEntity.getMonthlyIncome().doubleValue());
            employmentInformationModel.setVerified(employeeInformationEntity.isVerified());
            employmentInformationModel.setOrganizationName(employeeInformationEntity.getOrganizationName());
            employmentInformationModel.setOrganizationUrl(employeeInformationEntity.getOrganizationUrl());
            employmentInformationModel.setWorkEmail(employeeInformationEntity.getWorkEmail());

            loanCustomerProfileModel.setEmploymentInformation(employmentInformationModel);
        }

        return loanCustomerProfileModel;
    }
}
