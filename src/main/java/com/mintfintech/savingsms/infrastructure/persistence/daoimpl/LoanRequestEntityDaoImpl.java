package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.domain.models.LoanSearchDTO;
import com.mintfintech.savingsms.infrastructure.persistence.repository.LoanRequestRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Retryable;

import javax.inject.Named;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Named
public class LoanRequestEntityDaoImpl implements LoanRequestEntityDao {

    private final LoanRequestRepository repository;
    private final AppSequenceEntityDao appSequenceEntityDao;

    public LoanRequestEntityDaoImpl(LoanRequestRepository repository, AppSequenceEntityDao appSequenceEntityDao) {
        this.repository = repository;
        this.appSequenceEntityDao = appSequenceEntityDao;
    }

    private static Specification<LoanRequestEntity> withActiveStatus() {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("recordStatus"), RecordStatusConstant.ACTIVE)));
    }

    private static Specification<LoanRequestEntity> withDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return ((fundTransferRoot, criteriaQuery, criteriaBuilder) -> criteriaBuilder.between(fundTransferRoot.get("dateCreated"), startDate, endDate));
    }

    private static Specification<LoanRequestEntity> withMintBankAccount(MintBankAccountEntity account) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("bankAccount"), account.getId()));
    }

    private static Specification<LoanRequestEntity> withRepaymentStatus(LoanRepaymentStatusConstant loanStatus) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("repaymentStatus"), loanStatus));
    }

    private static Specification<LoanRequestEntity> withApprovalStatus(ApprovalStatusConstant approvalStatus) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("approvalStatus"), approvalStatus));
    }

    private static Specification<LoanRequestEntity> withLoanType(LoanTypeConstant loanType) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("loanType"), loanType));
    }


    @Override
    public Optional<LoanRequestEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public LoanRequestEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. LoanRequestEntity with Id :" + aLong));
    }

    @Override
    public LoanRequestEntity saveRecord(LoanRequestEntity record) {
        return repository.save(record);
    }

    @Override
    public long countActiveLoan(AppUserEntity appUserEntity) {
        return repository.countActiveCustomerLoanOnAccount(appUserEntity);
    }

    @Override
    public long countTotalLoans(AppUserEntity appUserEntity) {
        return repository.countTotalLoans(appUserEntity);
    }

    @Override
    public long countTotalLoansPastRepaymentDueDate(AppUserEntity appUserEntity) {
        return repository.countTotalLoansPastRepaymentDueDate(appUserEntity);
    }

    @Retryable
    @Override
    public String generateLoanId() {
        return String.format("%s%06d%s", RandomStringUtils.randomNumeric(1),
                appSequenceEntityDao.getNextSequenceId(SequenceType.LOAN_SEQ),
                RandomStringUtils.randomNumeric(1));
    }

    @Retryable
    @Override
    public String generateLoanTransactionRef() {
        return String.format("ML%09d%s",
                appSequenceEntityDao.getNextSequenceId(SequenceType.LOAN_TRANSACTION_REFERENCE_SEQ),
                RandomStringUtils.randomNumeric(1));
    }

    @Override
    public Page<LoanRequestEntity> searchLoans(LoanSearchDTO searchDTO, int pageIndex, int recordSize) {

        Pageable pageable = PageRequest.of(pageIndex, recordSize, Sort.by("dateCreated").descending());

        Specification<LoanRequestEntity> specification = withActiveStatus();

        if (searchDTO.getToDate() != null && searchDTO.getFromDate() != null) {
            specification = specification.and(withDateRange(searchDTO.getFromDate(), searchDTO.getToDate()));
        }
        if (searchDTO.getAccount() != null) {
            specification = specification.and(withMintBankAccount(searchDTO.getAccount()));
        }
        if (searchDTO.getRepaymentStatus() != null) {
            specification = specification.and(withRepaymentStatus(searchDTO.getRepaymentStatus()));
        }
        if (searchDTO.getApprovalStatus() != null) {
            specification = specification.and(withApprovalStatus(searchDTO.getApprovalStatus()));
        }
        if (searchDTO.getLoanType() != null){
            specification = specification.and(withLoanType(searchDTO.getLoanType()));
        }
        return repository.findAll(specification, pageable);
    }

    @Override
    public Optional<LoanRequestEntity> findByLoanId(String loanId) {
        return repository.findByLoanId(loanId);
    }

    @Override
    public List<LoanRequestEntity> getLoansByAppUser(AppUserEntity appUser, String loanType) {
        List<LoanRequestEntity> loanRequestEntities = new ArrayList<>();
        if (loanType.equalsIgnoreCase("ALL")) {
            loanRequestEntities = repository.getAllByRequestedByAndRecordStatus(appUser, RecordStatusConstant.ACTIVE);
        } else if (LoanTypeConstant.valueOf(loanType).equals(LoanTypeConstant.PAYDAY)) {
            loanRequestEntities = repository.getAllByRequestedByAndRecordStatusAndLoanType(appUser, RecordStatusConstant.ACTIVE, LoanTypeConstant.PAYDAY);
        }
        return loanRequestEntities;
    }

    @Override
    public List<LoanRequestEntity> getLoanRepaymentDueInDays(int days) {
        LocalDateTime to = LocalDateTime.now().plusDays(days).toLocalDate().atTime(LocalTime.MAX);
        LocalDateTime from = to.toLocalDate().atStartOfDay();
        return repository.getRepaymentPlansDueAtTime(from, to);
    }

    @Override
    public List<LoanRequestEntity> getLoanRepaymentDueToday() {
        LocalDateTime to = LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
        LocalDateTime from = to.toLocalDate().atStartOfDay();
        return repository.getRepaymentPlansDueAtTime(from, to);
    }

    @Override
    public List<LoanRequestEntity> getDefaultedUnpaidLoanRepayment() {
        return repository.getDefaultedUnpaidLoanRepayment();
    }
}
