package com.mintfintech.savingsms.usecase.features.investment.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentInterestEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentTransactionEntityDao;
import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentTransactionEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.models.InvestmentSearchDTO;
import com.mintfintech.savingsms.domain.models.InvestmentTransactionSearchDTO;
import com.mintfintech.savingsms.domain.models.reports.InvestmentStat;
import com.mintfintech.savingsms.domain.models.reports.SavingsMaturityStat;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.usecase.data.request.InvestmentSearchRequest;
import com.mintfintech.savingsms.usecase.data.request.InvestmentTransactionSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentMaturityStatModel;
import com.mintfintech.savingsms.usecase.data.response.InvestmentMaturityStatSummary;
import com.mintfintech.savingsms.usecase.data.response.InvestmentStatSummary;
import com.mintfintech.savingsms.usecase.data.response.InvestmentTransactionSearchResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import com.mintfintech.savingsms.usecase.models.InvestmentTransactionModel;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GetInvestmentUseCaseImpl implements GetInvestmentUseCase {

	private final InvestmentInterestEntityDao investmentInterestEntityDao;
	private final InvestmentEntityDao investmentEntityDao;
	private final AppUserEntityDao appUserEntityDao;
	private final MintAccountEntityDao mintAccountEntityDao;
	private final InvestmentTransactionEntityDao investmentTransactionEntityDao;
	private final ApplicationProperty applicationProperty;
	private final MintBankAccountEntityDao mintBankAccountEntityDao;

	@Override
	public List<InvestmentTransactionModel> getInvestmentTransactions(String investmentId) {

		InvestmentEntity investment = investmentEntityDao.findByCode(investmentId)
				.orElseThrow(() -> new NotFoundException("Invalid investment id " + investmentId));

		List<InvestmentTransactionEntity> transactionEntities = investmentTransactionEntityDao
				.getTransactionsByInvestment(investment);
		System.out.println("history size - " + transactionEntities.size());
		List<InvestmentTransactionModel> transactions = new ArrayList<>();

		transactionEntities.forEach(funding -> {
			InvestmentTransactionModel transaction = new InvestmentTransactionModel();
			transaction.setAmount(funding.getTransactionAmount());
			transaction.setDate(funding.getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE));
			transaction.setType(funding.getTransactionType().name());
			transaction.setTransactionStatus(funding.getTransactionStatus().name());
			transaction.setReference(funding.getTransactionReference());
			if (StringUtils.isNotEmpty(funding.getTransactionDescription())) {
				transaction.setDescription(funding.getTransactionDescription());
			} else {
				if (funding.getTransactionType() == TransactionTypeConstant.DEBIT) {
					transaction.setDescription("Investment debit.");
				} else {
					transaction.setDescription("Investment credit.");
				}
			}
			transactions.add(transaction);
		});

		transactions.sort(Comparator.comparing(o -> LocalDate.parse(o.getDate())));

		return transactions;
	}

	@Override
	public InvestmentMaturityStatSummary getMaturityStatistics(LocalDate fromDate, LocalDate toDate) {
		long days = fromDate.until(toDate, ChronoUnit.DAYS);
		if (days > 90) {
			throw new BusinessLogicConflictException("Sorry, Maximum of 90 days range is permitted");
		}
		if (fromDate.isBefore(LocalDate.now())) {
			throw new BusinessLogicConflictException("Start date cannot be before current date.");
		}
		LocalDateTime startDate = fromDate.atStartOfDay();
		LocalDateTime endDate = toDate.atTime(LocalTime.MAX);

		List<SavingsMaturityStat> maturityStatList = investmentEntityDao.getInvestmentMaturityStatistics(startDate,
				endDate);
		InvestmentMaturityStatSummary maturityStatModel = new InvestmentMaturityStatSummary();
		List<InvestmentMaturityStatModel> investmentMaturityStatList = new ArrayList<>();
		int year = fromDate.getYear();
		for (SavingsMaturityStat modelStat : maturityStatList) {
			LocalDate statDate = LocalDate.of(year, modelStat.getMonth(), modelStat.getDay());
			InvestmentMaturityStatModel maturityStat = new InvestmentMaturityStatModel();
			maturityStat.setMaturityDate(statDate.format(DateTimeFormatter.ISO_DATE));
			maturityStat.setTotalRecords(modelStat.getTotalRecords());
			maturityStat.setTotalAmount(modelStat.getTotalInterest().add(modelStat.getTotalSavings()));
			maturityStat.setTotalInterest(modelStat.getTotalInterest());
			maturityStat.setTotalInvested(modelStat.getTotalSavings());
			maturityStatModel.setTotalAmount(maturityStatModel.getTotalAmount().add(maturityStat.getTotalAmount()));
			maturityStatModel.setTotalInterest(maturityStatModel.getTotalInterest().add(modelStat.getTotalInterest()));
			maturityStatModel.setTotalInvested(maturityStatModel.getTotalInvested().add(modelStat.getTotalSavings()));
			maturityStatModel.setTotalRecords(maturityStatModel.getTotalRecords() + modelStat.getTotalRecords());
			investmentMaturityStatList.add(maturityStat);
		}
		maturityStatModel.setInvestmentMaturityStatList(investmentMaturityStatList);
		return maturityStatModel;
	}

	@Override
	public InvestmentStatSummary getPagedInvestmentsByAdmin(InvestmentSearchRequest searchRequest, int page, int size) {
		Optional<MintAccountEntity> mintAccount = mintAccountEntityDao
				.findAccountByAccountId(searchRequest.getAccountId());
		InvestmentStatusConstant status = null;
		if (StringUtils.isNotEmpty(searchRequest.getInvestmentStatus())) {
			status = InvestmentStatusConstant.valueOf(searchRequest.getInvestmentStatus());
		}

		InvestmentSearchDTO searchDTO = InvestmentSearchDTO.builder().startFromDate(
				searchRequest.getStartFromDate() != null ? searchRequest.getStartFromDate().atStartOfDay() : null)
				.startToDate(
						searchRequest.getStartToDate() != null ? searchRequest.getStartToDate().atTime(23, 59) : null)
				.duration(searchRequest.getDuration()).customerName(searchRequest.getCustomerName())
				.matureFromDate(
						searchRequest.getMatureFromDate() != null ? searchRequest.getMatureFromDate().atStartOfDay()
								: null)
				.matureToDate(
						searchRequest.getMatureToDate() != null ? searchRequest.getMatureToDate().atTime(23, 59) : null)
				.investmentStatus(status).account(mintAccount.orElse(null))
				.completedRecords(searchRequest.isCompletedRecords()).build();

		Page<InvestmentEntity> investmentEntityPage = investmentEntityDao.searchInvestments(searchDTO, page, size);
		BigDecimal totalInvestmentAmount = investmentEntityDao.sumSearchedInvestments(searchDTO);

		InvestmentStatSummary summary = new InvestmentStatSummary();
		summary.setInvestments(new PagedDataResponse<>(investmentEntityPage.getTotalElements(),
				investmentEntityPage.getTotalPages(), totalInvestmentAmount,
				investmentEntityPage.get().map(this::toInvestmentModel).collect(Collectors.toList())));

		return summary;
	}

	@Override
	public InvestmentModel toInvestmentModel(InvestmentEntity investment) {

		if (!Hibernate.isInitialized(investment)) {
			investment = investmentEntityDao.getRecordById(investment.getId());
		}

		InvestmentModel model = new InvestmentModel();

		AppUserEntity appUser = appUserEntityDao.getRecordById(investment.getCreator().getId());

		if (investment.getInvestmentStatus() == InvestmentStatusConstant.COMPLETED
				|| investment.getInvestmentStatus() == InvestmentStatusConstant.LIQUIDATED) {
			model.setAmountInvested(
					investment.getTotalAmountWithdrawn().subtract(investment.getTotalInterestWithdrawn()));
			model.setTotalAccruedInterest(investment.getTotalInterestWithdrawn());
			model.setTotalExpectedReturn(investment.getTotalAmountWithdrawn());
			model.setEstimatedProfitAtMaturity(investment.getTotalInterestWithdrawn());
		} else {
			BigDecimal totalAccruedInterest = investmentInterestEntityDao
					.getTotalInterestAmountOnInvestment(investment);
			model.setAmountInvested(
					investment.getInvestmentStatus() == InvestmentStatusConstant.ACTIVE ? investment.getAmountInvested()
							: investment.getTotalAmountInvested());
			model.setTotalAccruedInterest(totalAccruedInterest.setScale(2, BigDecimal.ROUND_HALF_UP));

			BigDecimal amountInvested = investment.getAmountInvested();
			double rate = investment.getInterestRate();
			BigDecimal accruedInterest = investment.getAccruedInterest();
			LocalDateTime maturityTime = investment.getMaturityDate();

			model.setTotalExpectedReturn(
					calculateTotalExpectedReturn(amountInvested, accruedInterest, rate, maturityTime));
			model.setEstimatedProfitAtMaturity(
					calculateOutstandingInterest(amountInvested, rate, maturityTime).add(accruedInterest));
		}

		model.setStatus(investment.getInvestmentStatus().name());
		model.setType(investment.getInvestmentType().name());
		model.setAccruedInterest(investment.getAccruedInterest());
		model.setWithholdingTax(investment.getAccruedInterest().multiply(BigDecimal.valueOf(0.1)));

		int minimumLiquidationPeriodInDays = applicationProperty.investmentMinimumLiquidationDays();
		if (!applicationProperty.isLiveEnvironment()) {
			minimumLiquidationPeriodInDays = 2;
		}
		boolean canLiquidate = false;
		if (investment.getInvestmentStatus() == InvestmentStatusConstant.ACTIVE) {
			long daysPast = investment.getDateCreated().until(LocalDateTime.now(), ChronoUnit.DAYS);
			if (daysPast >= minimumLiquidationPeriodInDays) {
				canLiquidate = true;
			}
		}
		model.setCanLiquidate(canLiquidate);
		model.setInterestRate(investment.getInterestRate());
		model.setCode(investment.getCode());
		model.setDateWithdrawn(investment.getDateWithdrawn() != null
				? investment.getDateWithdrawn().format(DateTimeFormatter.ISO_LOCAL_DATE)
				: null);
		model.setMaturityDate(investment.getMaturityDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
		model.setDurationInMonths(investment.getDurationInMonths());
		model.setStartDate(investment.getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE));
		model.setTenorName(investment.getInvestmentTenor().getDurationDescription());
		model.setTotalAmountWithdrawn(investment.getTotalAmountWithdrawn());
		model.setPenaltyCharge(investment.getInvestmentTenor().getPenaltyRate());
		model.setMaxLiquidateRate(investment.getMaxLiquidateRate());
		model.setCustomerName(investment.getOwner().getName());
		model.setUserId(appUser.getUserId());

		return model;
	}

	@Override
	public InvestmentStatSummary getPagedInvestments(InvestmentSearchRequest searchRequest, int page, int size) {

		Optional<MintAccountEntity> mintAccount = mintAccountEntityDao
				.findAccountByAccountId(searchRequest.getAccountId());
		InvestmentStatusConstant status = null;
		if (StringUtils.isNotEmpty(searchRequest.getInvestmentStatus())) {
			status = InvestmentStatusConstant.valueOf(searchRequest.getInvestmentStatus());
		}

		InvestmentSearchDTO searchDTO = InvestmentSearchDTO.builder().startFromDate(
				searchRequest.getStartFromDate() != null ? searchRequest.getStartFromDate().atStartOfDay() : null)
				.startToDate(
						searchRequest.getStartToDate() != null ? searchRequest.getStartToDate().atTime(23, 59) : null)
				.duration(searchRequest.getDuration()).customerName(searchRequest.getCustomerName())
				.matureFromDate(
						searchRequest.getMatureFromDate() != null ? searchRequest.getMatureFromDate().atStartOfDay()
								: null)
				.matureToDate(
						searchRequest.getMatureToDate() != null ? searchRequest.getMatureToDate().atTime(23, 59) : null)
				.investmentStatus(status).account(mintAccount.orElse(null))
				.completedRecords(searchRequest.isCompletedRecords()).accountType(searchRequest.getAccountType())
				.build();

		Page<InvestmentEntity> investmentEntityPage = investmentEntityDao.searchInvestments(searchDTO, page, size);
		BigDecimal totalInvestmentAmount = investmentEntityDao.sumSearchedInvestments(searchDTO);

		InvestmentStatSummary summary = new InvestmentStatSummary();
		summary.setInvestments(new PagedDataResponse<>(investmentEntityPage.getTotalElements(),
				investmentEntityPage.getTotalPages(), totalInvestmentAmount,
				investmentEntityPage.get().map(this::toInvestmentModel).collect(Collectors.toList())));

		if (mintAccount.isPresent()) {

			if (status != null) {

				if (status == InvestmentStatusConstant.COMPLETED) {
					List<InvestmentStat> stats = investmentEntityDao.getStatsForCompletedInvestment(mintAccount.get());

					for (InvestmentStat stat : stats) {
						if (stat.getInvestmentStatus().equals(InvestmentStatusConstant.LIQUIDATED)
								|| stat.getInvestmentStatus().equals(InvestmentStatusConstant.COMPLETED)) {
							summary.setTotalInvestments(summary.getTotalInvestments() + stat.getTotalRecords());
							summary.setTotalInvested(summary.getTotalInvested().add(stat.getTotalInvestment()));
							summary.setTotalExpectedReturns(
									summary.getTotalExpectedReturns().add(stat.getTotalExpectedReturns()));
						}
					}
				} else {
					List<InvestmentStat> stats = investmentEntityDao.getInvestmentStatOnAccount(mintAccount.get());

					for (InvestmentStat stat : stats) {
						if (stat.getInvestmentStatus().equals(status)) {
							summary.setTotalInvestments(stat.getTotalRecords());
							summary.setTotalInvested(stat.getTotalInvestment());
							summary.setTotalProfit(
									stat.getAccruedInterest().add(BigDecimal.valueOf(stat.getOutstandingInterest()))
											.setScale(2, BigDecimal.ROUND_HALF_EVEN));
							summary.setTotalExpectedReturns(summary.getTotalInvested().add(summary.getTotalProfit())
									.setScale(2, BigDecimal.ROUND_HALF_EVEN));
						}
					}
				}
			} else {
				List<InvestmentEntity> investments = investmentEntityDao.getRecordsOnAccount(mintAccount.get());

				long totalRecords = investmentEntityPage.getTotalElements();

				BigDecimal totalInvestment = BigDecimal.ZERO;

				BigDecimal accruedInterest = BigDecimal.ZERO;

				BigDecimal outstandingInterest = BigDecimal.ZERO;

				for (InvestmentEntity investment : investments) {
					totalInvestment = totalInvestment.add(investment.getAmountInvested());
					accruedInterest = accruedInterest.add(investment.getAccruedInterest());
					outstandingInterest = outstandingInterest
							.add(calculateOutstandingInterest(investment.getAmountInvested(),
									investment.getInterestRate(), investment.getMaturityDate()));
				}

				summary.setTotalInvestments(totalRecords);
				summary.setTotalInvested(totalInvestment.setScale(2, BigDecimal.ROUND_HALF_EVEN));
				summary.setTotalProfit(
						accruedInterest.add(outstandingInterest).setScale(2, BigDecimal.ROUND_HALF_EVEN));
				summary.setTotalExpectedReturns(
						totalInvestment.add(summary.getTotalProfit()).setScale(2, BigDecimal.ROUND_HALF_EVEN));

			}
		}
		return summary;
	}

	@Override
	public BigDecimal calculateOutstandingInterest(BigDecimal amountInvested, double interestRate,
			LocalDateTime maturityTime) {

		double interestPerAnum = interestRate * 0.01 * amountInvested.doubleValue();

		double dailyInterest = interestPerAnum / 365.0;

		LocalDate maturityDate = maturityTime.toLocalDate();

		LocalDate today = LocalDate.now();

		long remainingDaysToMaturity = ChronoUnit.DAYS.between(today, maturityDate);

		double outstandingInterest = dailyInterest * remainingDaysToMaturity;

		return BigDecimal.valueOf(outstandingInterest).setScale(2, BigDecimal.ROUND_HALF_EVEN);
	}

	@Override
	public BigDecimal calculateTotalExpectedReturn(BigDecimal amountInvested, BigDecimal currentAccruedInterest,
			double interestRate, LocalDateTime maturityTime) {

		BigDecimal interestToAccumulate = calculateOutstandingInterest(amountInvested, interestRate, maturityTime);

		BigDecimal totalExpectedReturns = amountInvested.add(currentAccruedInterest).add(interestToAccumulate);

		return totalExpectedReturns.setScale(2, BigDecimal.ROUND_HALF_EVEN);
	}

	@Override
	public PagedDataResponse<InvestmentTransactionSearchResponse> getInvestmentTransactions(
			InvestmentTransactionSearchRequest request, int pageIndex, int size) {
		InvestmentTransactionSearchDTO searchDTO = InvestmentTransactionSearchDTO.builder()
				.fromDate(request.getFromDate()).toDate(request.getToDate())
				.transactionAmount(request.getTransactionAmount()).transactionStatus(request.getTransactionStatus())
				.mintAccountNumber(request.getMintAccountNumber())
				.transactionReference(request.getTransactionReference()).transactionType(request.getTransactionType())
				.accountType(request.getAccountType())
				.name(request.getName())
				.build();
		Page<InvestmentTransactionEntity> pagedEntity = investmentTransactionEntityDao.searchInvestmentTransactions(searchDTO, pageIndex, size);
		BigDecimal amount = investmentTransactionEntityDao.sumSearchedInvestmentTransactions(searchDTO);
		return new PagedDataResponse<>(pagedEntity.getTotalElements(), pagedEntity.getTotalPages(), amount,
				pagedEntity.getContent().stream().map(this::getResponseFromEntity).collect(Collectors.toList()));
	}

	private InvestmentTransactionSearchResponse getResponseFromEntity(InvestmentTransactionEntity entity) {
		MintBankAccountEntity mintBankAccountEntity = null;
		//MintAccountEntity mintAccount = null;
		if (entity.getBankAccount() != null) {
			mintBankAccountEntity = mintBankAccountEntityDao.getRecordById(entity.getBankAccount().getId());
			//mintAccount = mintAccountEntityDao.getRecordById(mintBankAccountEntity.getMintAccount().getId());
		}
		return InvestmentTransactionSearchResponse.builder()
				.customerAccountNumber((mintBankAccountEntity != null) ? mintBankAccountEntity.getAccountNumber() : "")
				.customerName((mintBankAccountEntity != null) ? mintBankAccountEntity.getAccountName() : "")
				.investmentBalance(entity.getCurrentBalance()).responseCode(entity.getTransactionResponseCode())
				.responseMessage(entity.getTransactionResponseMessage())
				.transactionDate(entity.getDateCreated().toString())
				.transactionStatus(entity.getTransactionStatus().name())
				.transactionType(entity.getTransactionType().name()).transactionAmount(entity.getTransactionAmount())
				.bankAccountType(mintBankAccountEntity.getAccountType().name())
				.accountType(mintBankAccountEntity.getAccountGroup().name())
				.build();
	}
}
