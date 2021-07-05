package com.mintfintech.savingsms.usecase.features.loan.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.CustomerLoanProfileEntityDao;
import com.mintfintech.savingsms.domain.dao.EmployeeInformationEntityDao;
import com.mintfintech.savingsms.domain.dao.ResourceFileEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CustomerLoanProfileEntity;
import com.mintfintech.savingsms.domain.entities.EmployeeInformationEntity;
import com.mintfintech.savingsms.domain.entities.ResourceFileEntity;
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.usecase.data.events.outgoing.EmploymentInfoUpdateEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.LoanEmailEvent;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.features.loan.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.features.loan.UpdateEmploymentInfoUseCase;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;
import com.mintfintech.savingsms.utils.PhoneNumberUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Wed, 23 Jun, 2021
 */
@Slf4j
@Named
@AllArgsConstructor
public class UpdateEmploymentInfoUseCaseImpl implements UpdateEmploymentInfoUseCase {

    private final AppUserEntityDao appUserEntityDao;
    private final CustomerLoanProfileEntityDao customerLoanProfileEntityDao;
    private final EmployeeInformationEntityDao employeeInformationEntityDao;
    private final ApplicationProperty applicationProperty;
    private final ApplicationEventService applicationEventService;
    private final ResourceFileEntityDao resourceFileEntityDao;

    @Override
    public void updateCustomerEmploymentInformation(EmploymentInfoUpdateEvent updateEvent) {


        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(updateEvent.getUserId());

        Optional<CustomerLoanProfileEntity> customerLoanProfileEntityOpt = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser);
        if(!customerLoanProfileEntityOpt.isPresent()) {
            log.info("Not loan profile created for customer - {}", updateEvent.getUserId());
            return;
        }
        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityOpt.get();
        if (customerLoanProfileEntity.getEmployeeInformation() == null) {
            log.info("An Employment Information does not exists for this user. - {}", updateEvent.getUserId());
            return;
        }

        EmployeeInformationEntity employeeInfo = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());

        employeeInfo.setVerificationStatus(ApprovalStatusConstant.PENDING);
        employeeInfo.setEmployerEmail(updateEvent.getEmployerEmail());
        employeeInfo.setEmployerAddress(updateEvent.getEmployerAddress());
        employeeInfo.setEmployerPhoneNo(PhoneNumberUtils.toInternationalFormat(updateEvent.getEmployerPhoneNumber()));
        employeeInfo.setMonthlyIncome(updateEvent.getMonthlySalary());
        employeeInfo.setOrganizationName(updateEvent.getOrganizationName());
        employeeInfo.setWorkEmail(updateEvent.getWorkEmail());
        employeeInfo.setOrganizationUrl(updateEvent.getOrganizationUrl());
        employeeInformationEntityDao.saveRecord(employeeInfo);

        if(updateEvent.getEmploymentLetterFileSize() > 0) {
            ResourceFileEntity employmentLetter = resourceFileEntityDao.getRecordById(employeeInfo.getEmploymentLetter().getId());
            employmentLetter.setFileId(updateEvent.getEmploymentLetterFileId());
            employmentLetter.setFileName(updateEvent.getEmploymentLetterFileName());
            employmentLetter.setFileSizeInKB(updateEvent.getEmploymentLetterFileSize());
            employmentLetter.setRemoteResource(true);
            employmentLetter.setUrl(updateEvent.getEmploymentLetterFileUrl());
            resourceFileEntityDao.saveRecord(employmentLetter);
        }

        LoanEmailEvent loanEmailEvent = LoanEmailEvent.builder()
                .recipient(applicationProperty.getSystemAdminEmail())
                .customerName(appUser.getName())
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_PROFILE_UPDATE_ADMIN, new EventModel<>(loanEmailEvent));
    }
}
