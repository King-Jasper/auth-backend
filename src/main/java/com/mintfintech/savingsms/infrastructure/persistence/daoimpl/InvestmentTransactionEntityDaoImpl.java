package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import java.math.BigDecimal;
import java.util.List;

import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentTransactionEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.models.InvestmentTransactionSearchDTO;
import com.mintfintech.savingsms.domain.models.reports.AmountModel;
import com.mintfintech.savingsms.infrastructure.persistence.repository.InvestmentTransactionRepository;

@Named
public class InvestmentTransactionEntityDaoImpl extends CrudDaoImpl<InvestmentTransactionEntity, Long>
		implements InvestmentTransactionEntityDao {

	private final InvestmentTransactionRepository repository;
	private final AppSequenceEntityDao appSequenceEntityDao;
	private final EntityManager entityManager;

	public InvestmentTransactionEntityDaoImpl(InvestmentTransactionRepository repository,
			AppSequenceEntityDao appSequenceEntityDao, EntityManager entityManager) {
		super(repository);
		this.repository = repository;
		this.appSequenceEntityDao = appSequenceEntityDao;
		this.entityManager = entityManager;
	}

	@Override
	public String generateTransactionReference() {
		return String.format("MI%09d%s",
				appSequenceEntityDao.getNextSequenceId(SequenceType.INVESTMENT_TRANSACTION_REFERENCE_SEQ),
				RandomStringUtils.randomNumeric(1));
	}

	@Override
	public List<InvestmentTransactionEntity> getTransactionsByInvestment(InvestmentEntity investmentEntity,
			TransactionTypeConstant type, TransactionStatusConstant status) {
		return repository.getAllByInvestmentAndTransactionTypeAndTransactionStatusOrderByDateCreatedDesc(
				investmentEntity, type, status);
	}

	@Override
	public List<InvestmentTransactionEntity> getTransactionsByInvestment(InvestmentEntity investmentEntity) {
		return repository.getAllByRecordStatusAndInvestmentOrderByDateCreatedDesc(RecordStatusConstant.ACTIVE,
				investmentEntity);
	}

	@Override
	public Page<InvestmentTransactionEntity> searchInvestmentTransactions(
			InvestmentTransactionSearchDTO investmentTransactionSearchDTO, int pageIndex, int size) {
		pageIndex = ((pageIndex <= 0 ? 0 : pageIndex - 1) * size);
		Pageable pageable = PageRequest.of(pageIndex, size, Sort.by("dateCreated").descending());
		Specification<InvestmentTransactionEntity> specification = (root, query,
				criteriaBuilder) -> buildSearchQuery(investmentTransactionSearchDTO, root, query, criteriaBuilder);
		return repository.findAll(specification, pageable);
	}

	@Override
	public BigDecimal sumSearchedInvestmentTransactions(InvestmentTransactionSearchDTO investmentTransactionSearchDTO) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<AmountModel> query = cb.createQuery(AmountModel.class);
		Root<InvestmentTransactionEntity> root = query.from(InvestmentTransactionEntity.class);
		Predicate whereClause = buildSearchQuery(investmentTransactionSearchDTO, root, query, cb);

		query.select(cb.construct(AmountModel.class, cb.sum(root.get("transactionAmount"))));
		query.where(whereClause);

		TypedQuery<AmountModel> typedQuery = entityManager.createQuery(query);
		BigDecimal amount = typedQuery.getSingleResult().getAmount();
		return amount == null ? BigDecimal.ZERO : amount;
	}

	private Predicate buildSearchQuery(InvestmentTransactionSearchDTO searchDTO, Root<InvestmentTransactionEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
		Predicate whereClause = cb.equal(root.get("recordStatus"), RecordStatusConstant.ACTIVE);
		if (searchDTO.getToDate() != null && searchDTO.getFromDate() != null) {
			whereClause = cb.and(whereClause,
					cb.between(root.get("dateCreated"), searchDTO.getFromDate(), searchDTO.getToDate()));
		}
		if (!StringUtils.isEmpty(searchDTO.getMintAccountNumber())) {
			Join<InvestmentTransactionEntity, MintBankAccountEntity> bankAccountSpec = root.join("bankAccount");
			whereClause = cb.and(whereClause,
					cb.equal(bankAccountSpec.get("accountNumber"), searchDTO.getMintAccountNumber()));
		}
		if (searchDTO.getTransactionAmount().compareTo(BigDecimal.ZERO) > 0) {
			whereClause = cb.and(whereClause,
					cb.equal(root.get("transactionAmount"), searchDTO.getTransactionAmount()));
		}
		if (searchDTO.getTransactionStatus() != null) {
			whereClause = cb.and(whereClause,
					cb.equal(root.get("transactionStatus"), searchDTO.getTransactionStatus()));
		}
		if (searchDTO.getTransactionType() != null) {
			whereClause = cb.and(whereClause, cb.equal(root.get("transactionType"), searchDTO.getTransactionType()));
		}
		if (StringUtils.isNotEmpty(searchDTO.getTransactionReference())) {
			whereClause = cb.and(whereClause,
					cb.equal(root.get("externalReference"), searchDTO.getTransactionReference()));
		}
		return whereClause;
	}

}
