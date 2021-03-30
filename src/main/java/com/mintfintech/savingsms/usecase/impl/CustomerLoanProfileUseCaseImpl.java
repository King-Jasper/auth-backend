package com.mintfintech.savingsms.usecase.impl;

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
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.AuditTrailService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.ImageResourceUseCase;
import com.mintfintech.savingsms.usecase.LoanUseCase;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.models.EmploymentInformationModel;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;
import com.mintfintech.savingsms.utils.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
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
    private final GetLoansUseCase getLoansUseCase;
    private final LoanUseCase loanUseCase;

    @Override
    @Transactional
    public LoanCustomerProfileModel payDayProfileCreationWithLoanRequest(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request) {

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
                    .appUser(appUser)
                    .employeeInformation(employeeInformationEntity)
                    .build();
            customerLoanProfileEntity = customerLoanProfileEntityDao.saveRecord(newCustomerLoanProfile);
        }

        loanUseCase.loanRequest(currentUser, request.getLoanAmount(), "PAYDAY");

        List<LoanRequestEntity> loanRequestEntities = loanRequestEntityDao.getLoansByAppUser(appUser, "PAYDAY");

        return buildLoanCustomerProfileModel(customerLoanProfileEntity, LoanTypeConstant.valueOf("PAYDAY"), loanRequestEntities);
    }

    @Override
    public LoanCustomerProfileModel getLoanCustomerProfile(AuthenticatedUser currentUser, String loanType, String loanListType) {

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser).orElseThrow(
                () -> new BadRequestException("No Customer Loan Profile exists for this")
        );

        List<LoanRequestEntity> loanRequestEntities = loanRequestEntityDao.getLoansByAppUser(appUser, loanListType);

        return buildLoanCustomerProfileModel(customerLoanProfileEntity, LoanTypeConstant.valueOf(loanType), loanRequestEntities);
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

        return buildLoanCustomerProfileModel(customerLoanProfileEntity, LoanTypeConstant.valueOf("PAYDAY"), Collections.emptyList());
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

        return buildLoanCustomerProfileModel(customerLoanProfileEntity, LoanTypeConstant.valueOf("PAYDAY"), Collections.emptyList());
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

    private LoanCustomerProfileModel buildLoanCustomerProfileModel(
            CustomerLoanProfileEntity customerLoanProfileEntity,
            LoanTypeConstant loanType,
            List<LoanRequestEntity> loanRequestEntities
    ){
        LoanCustomerProfileModel loanCustomerProfileModel = new LoanCustomerProfileModel();
        loanCustomerProfileModel.setBlacklisted(customerLoanProfileEntity.isBlacklisted());
        loanCustomerProfileModel.setBlacklistReason(customerLoanProfileEntity.getBlacklistReason());
        loanCustomerProfileModel.setId(customerLoanProfileEntity.getId());
        loanCustomerProfileModel.setRating(customerLoanProfileEntity.getRating());
        loanCustomerProfileModel.setLoans(loanRequestEntities.stream().map(getLoansUseCase::toLoanModel).collect(Collectors.toList()));

        if (loanType.equals(LoanTypeConstant.PAYDAY)){
            loanCustomerProfileModel.setMaxLoanPercent(applicationProperty.getPayDayMaxLoanPercentAmount());
            loanCustomerProfileModel.setInterestRate(applicationProperty.getPayDayLoanInterestRate());

            if (customerLoanProfileEntity.getEmployeeInformation() != null) {
                EmployeeInformationEntity employeeInformationEntity = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());
                EmploymentInformationModel employmentInformationModel = new EmploymentInformationModel();

                ResourceFileEntity resourceFileEntity = resourceFileEntityDao.getRecordById(employeeInformationEntity.getEmploymentLetter().getId());

                employmentInformationModel.setEmployerEmail(employeeInformationEntity.getEmployerEmail());
                employmentInformationModel.setEmploymentLetterUrl(resourceFileEntity.getUrl());
                employmentInformationModel.setEmployerAddress(employeeInformationEntity.getEmployerAddress());
                employmentInformationModel.setEmployerPhoneNo(PhoneNumberUtils.toNationalFormat(employeeInformationEntity.getEmployerPhoneNo()));
                employmentInformationModel.setMonthlyIncome(employeeInformationEntity.getMonthlyIncome().doubleValue());
                employmentInformationModel.setVerified(employeeInformationEntity.isVerified());
                employmentInformationModel.setOrganizationName(employeeInformationEntity.getOrganizationName());
                employmentInformationModel.setOrganizationUrl(employeeInformationEntity.getOrganizationUrl());
                employmentInformationModel.setWorkEmail(employeeInformationEntity.getWorkEmail());

                loanCustomerProfileModel.setEmploymentInformation(employmentInformationModel);
            }
        }

        return loanCustomerProfileModel;
    }


}
