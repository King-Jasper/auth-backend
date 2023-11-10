package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsWithdrawalRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.WithdrawalRequestStatusConstant;
import com.mintfintech.savingsms.domain.models.SavingsSearchDTO;
import com.mintfintech.savingsms.usecase.GetSavingsWithdrawalUseCase;
import com.mintfintech.savingsms.usecase.data.request.SavingsSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalWithdrawalResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;

import javax.inject.Named;
import java.util.stream.Collectors;

@FieldDefaults(makeFinal = true)
@AllArgsConstructor
@Named
public class GetSavingsWithdrawalUseCaseImpl implements GetSavingsWithdrawalUseCase {

    private SavingsWithdrawalRequestEntityDao savingsWithdrawalRequestEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    @Override
    public PagedDataResponse<SavingsGoalWithdrawalResponse> getSavingsGoalWithdrawalReport(SavingsSearchRequest searchRequest, int page, int size) {
        MintBankAccountEntity accountEntity= mintBankAccountEntityDao.findByAccountName(searchRequest.getCustomerName()).orElseThrow(()->new BadRequestException("Invalid customer name"));

        SavingsSearchDTO searchDTO = SavingsSearchDTO.builder()
                .customerName(searchRequest.getCustomerName())
                .withdrawalStatus(WithdrawalRequestStatusConstant.valueOf(searchRequest.getWithdrawalStatus()))
                .fromDate(searchRequest.getFromDate() != null ? searchRequest.getFromDate().atStartOfDay() : null)
                .toDate(searchRequest.getToDate() != null ? searchRequest.getToDate().atTime(23, 59): null)
                .build();
        Page<SavingsWithdrawalRequestEntity> goalEntityPage = savingsWithdrawalRequestEntityDao.getSavingsWithdrawalReport(searchDTO, page, size);

        return new PagedDataResponse<>(
                goalEntityPage.getTotalElements(),
                goalEntityPage.getTotalPages(),
                goalEntityPage.get().map(savings -> fromSavingsWithdrawalToSavingsGoalWithdrawalResponse(savings,accountEntity))
                        .collect(Collectors.toList()));
    }

    private SavingsGoalWithdrawalResponse fromSavingsWithdrawalToSavingsGoalWithdrawalResponse(SavingsWithdrawalRequestEntity withdrawalRequest, MintBankAccountEntity accountEntity){
        return SavingsGoalWithdrawalResponse.builder()
                .withdrawalDate(withdrawalRequest.getDateForWithdrawal())
                .amountWithdrawal(withdrawalRequest.getAmount())
                .savingsAmount(withdrawalRequest.getSavingsGoal().getSavingsAmount())
                .interestAmount(withdrawalRequest.getInterestWithdrawal())
                .withholdingTax(withdrawalRequest.getWithholdingTax())
                .withdrawalStatus(withdrawalRequest.getWithdrawalRequestStatus().name())
                .savingsName(withdrawalRequest.getSavingsGoal().getName())
                .goalId(withdrawalRequest.getSavingsGoal().getGoalId())
                .customerName(accountEntity.getAccountName())
                .accountNumber(accountEntity.getAccountNumber())
                .savingsType(withdrawalRequest.getSavingsGoal().getSavingsGoalType().name())
                .startDate(withdrawalRequest.getSavingsGoal().getSavingsStartDate())
                .maturityDate(withdrawalRequest.getSavingsGoal().getMaturityDate())
                .duration(withdrawalRequest.getSavingsGoal().getSelectedDuration())
                .interestRate(withdrawalRequest.getSavingsGoal().getInterestRate())
                .build();
    }
}
