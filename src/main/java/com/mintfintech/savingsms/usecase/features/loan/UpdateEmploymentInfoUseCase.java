package com.mintfintech.savingsms.usecase.features.loan;

import com.mintfintech.savingsms.usecase.data.events.outgoing.EmploymentInfoUpdateEvent;

/**
 * Created by jnwanya on
 * Wed, 23 Jun, 2021
 */
public interface UpdateEmploymentInfoUseCase {
    void updateCustomerEmploymentInformation(EmploymentInfoUpdateEvent employmentInfoUpdateEvent);
}
