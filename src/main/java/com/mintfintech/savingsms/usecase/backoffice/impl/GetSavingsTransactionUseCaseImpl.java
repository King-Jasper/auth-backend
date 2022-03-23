package com.mintfintech.savingsms.usecase.backoffice.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.springframework.data.domain.Page;

import com.mintfintech.savingsms.domain.dao.InvestmentTransactionEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.entities.InvestmentTransactionEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.models.InvestmentTransactionSearchDTO;
import com.mintfintech.savingsms.domain.models.reports.SavingsMaturityStat;
import com.mintfintech.savingsms.usecase.backoffice.GetSavingsTransactionUseCase;
import com.mintfintech.savingsms.usecase.data.request.InvestmentTransactionSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentTransactionSearchResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.SavingsMaturityStatModel;
import com.mintfintech.savingsms.usecase.data.response.SavingsMaturityStatSummary;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Created by jnwanya on Mon, 22 Jun, 2020
 */

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Named
@AllArgsConstructor
public class GetSavingsTransactionUseCaseImpl implements GetSavingsTransactionUseCase {

	SavingsGoalEntityDao savingsGoalEntityDao;
	InvestmentTransactionEntityDao investmentTransactionEntityDao;
	MintBankAccountEntityDao mintBankAccountEntityDao;

	@Override
	public SavingsMaturityStatSummary getSavingsMaturityStatistics(LocalDate fromDate, LocalDate toDate) {
		long days = fromDate.until(toDate, ChronoUnit.DAYS);
		if (days > 90) {
			throw new BusinessLogicConflictException("Sorry, Maximum of 90 days range is permitted");
		}
		if (fromDate.isBefore(LocalDate.now())) {
			throw new BusinessLogicConflictException("Start date cannot be before current date.");
		}
		LocalDateTime startDate = fromDate.atStartOfDay();
		LocalDateTime endDate = toDate.atTime(LocalTime.MAX);
		List<SavingsMaturityStat> maturityStatList = savingsGoalEntityDao.savingsMaturityStatisticsList(startDate,
				endDate);
		SavingsMaturityStatSummary maturityStatModel = new SavingsMaturityStatSummary();
		List<SavingsMaturityStatModel> savingsMaturityStatList = new ArrayList<>();
		int year = fromDate.getYear();
		for (SavingsMaturityStat modelStat : maturityStatList) {
			LocalDate statDate = LocalDate.of(year, modelStat.getMonth(), modelStat.getDay());
			SavingsMaturityStatModel maturityStat = new SavingsMaturityStatModel();
			maturityStat.setMaturityDate(statDate.format(DateTimeFormatter.ISO_DATE));
			maturityStat.setTotalRecords(modelStat.getTotalRecords());
			maturityStat.setTotalAmount(modelStat.getTotalInterest().add(modelStat.getTotalSavings()));
			maturityStat.setTotalInterest(modelStat.getTotalInterest());
			maturityStat.setTotalSavings(modelStat.getTotalSavings());
			maturityStatModel.setTotalAmount(maturityStatModel.getTotalAmount().add(maturityStat.getTotalAmount()));
			maturityStatModel.setTotalInterest(maturityStatModel.getTotalInterest().add(modelStat.getTotalInterest()));
			maturityStatModel.setTotalSavings(maturityStatModel.getTotalSavings().add(modelStat.getTotalSavings()));
			maturityStatModel
					.setTotalSavingsRecord(maturityStatModel.getTotalSavingsRecord() + modelStat.getTotalRecords());
			savingsMaturityStatList.add(maturityStat);
		}
		/*
		 * for(int i = 0; i <= days; i++) { final int day = fromDate.getDayOfMonth();
		 * final int month = fromDate.getMonthValue(); SavingsMaturityStatModel
		 * maturityStat = new SavingsMaturityStatModel();
		 * maturityStat.setMaturityDate(fromDate.format(DateTimeFormatter.ISO_DATE));
		 * Optional<SavingsMaturityStat> statOptional =
		 * maturityStatList.stream().filter(modelStat -> month == modelStat.getMonth()
		 * && day == modelStat.getDay()).findFirst(); statOptional.ifPresent(modelStat
		 * -> { maturityStat.setTotalRecords(modelStat.getTotalRecords());
		 * maturityStat.setTotalAmount(modelStat.getTotalInterest().add(modelStat.
		 * getTotalSavings()));
		 * maturityStat.setTotalInterest(modelStat.getTotalInterest());
		 * maturityStat.setTotalSavings(modelStat.getTotalSavings());
		 * maturityStatModel.setTotalAmount(maturityStatModel.getTotalAmount().add(
		 * maturityStat.getTotalAmount()));
		 * maturityStatModel.setTotalInterest(maturityStatModel.getTotalInterest().add(
		 * modelStat.getTotalInterest()));
		 * maturityStatModel.setTotalSavings(maturityStatModel.getTotalSavings().add(
		 * modelStat.getTotalSavings()));
		 * maturityStatModel.setTotalSavingsRecord(maturityStatModel.
		 * getTotalSavingsRecord() + modelStat.getTotalRecords()); });
		 * savingsMaturityStatList.add(maturityStat); fromDate = fromDate.plusDays(1); }
		 */
		maturityStatModel.setSavingsMaturityStatList(savingsMaturityStatList);
		return maturityStatModel;
	}

	@Override
	public PagedDataResponse<InvestmentTransactionSearchResponse> getInvestmentTransactions(
			InvestmentTransactionSearchRequest request, int pageIndex, int size) {
		InvestmentTransactionSearchDTO searchDTO = InvestmentTransactionSearchDTO.builder()
				.fromDate(request.getFromDate()).toDate(request.getToDate())
				.transactionAmount(request.getTransactionAmount()).transactionStatus(request.getTransactionStatus())
				.mintAccountNumber(request.getMintAccountNumber())
				.transactionReference(request.getTransactionReference()).transactionType(request.getTransactionType())
				.build();
		Page<InvestmentTransactionEntity> pagedEntity = investmentTransactionEntityDao.searchInvestmentTransactions(searchDTO, pageIndex, size);
		BigDecimal amount = investmentTransactionEntityDao.sumSearchedInvestmentTransactions(searchDTO);
		return new PagedDataResponse<>(pagedEntity.getTotalElements(), pagedEntity.getTotalPages(), amount,
				pagedEntity.getContent().stream().map(this::getResponseFromEntity).collect(Collectors.toList()));
	}

	private InvestmentTransactionSearchResponse getResponseFromEntity(InvestmentTransactionEntity entity) {
		MintBankAccountEntity mintBankAccountEntity = null;
		if (entity.getBankAccount() != null) {
			mintBankAccountEntity = mintBankAccountEntityDao.getRecordById(entity.getBankAccount().getId());
		}
		return InvestmentTransactionSearchResponse.builder()
				.customerAccountNumber((mintBankAccountEntity != null) ? mintBankAccountEntity.getAccountNumber() : "")
				.customerName((mintBankAccountEntity != null) ? mintBankAccountEntity.getAccountName() : "")
				.investmentBalance(entity.getCurrentBalance()).responseCode(entity.getTransactionResponseCode())
				.responseMessage(entity.getTransactionResponseMessage())
				.transactionDate(entity.getDateCreated().toString())
				.transactionStatus(entity.getTransactionStatus().name())
				.transactionType(entity.getTransactionType().name()).transactionAmount(entity.getTransactionAmount())
				.build();
	}

}
