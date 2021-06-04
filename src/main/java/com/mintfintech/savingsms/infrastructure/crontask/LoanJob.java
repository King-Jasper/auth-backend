package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.features.loan.LoanApprovalUseCase;
import com.mintfintech.savingsms.usecase.features.loan.LoanRepaymentUseCase;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Named;

@Slf4j
@Named
public class LoanJob {

    private final LoanRepaymentUseCase loanRepaymentUseCase;
    private final LoanApprovalUseCase approvalUseCase;

    public LoanJob(LoanRepaymentUseCase loanRepaymentUseCase, LoanApprovalUseCase approvalUseCase) {
        this.loanRepaymentUseCase = loanRepaymentUseCase;
        this.approvalUseCase = approvalUseCase;
    }

    @SchedulerLock(name = "LoanJob_sendNotificationForDuePayments", lockAtMostForString = "PT45M")
    @Scheduled(cron = "0 01 00 1/1 * ?", zone = "Africa/Lagos") // runs every day at 00:01am.
    public void sendNotificationForDuePayments() {
        log.info("Sending email notification to all customers with loan payment due");
        loanRepaymentUseCase.dispatchEmailToCustomersWithPaymentDueInTwoDays();
    }

    @SchedulerLock(name = "LoanJob_runDueLoanRepaymentCheckService", lockAtMostForString = "PT45M")
    @Scheduled(cron = "0 00 12 1/1 * ?") // runs every day at 12:00pm.
    public void runDueLoanRepaymentCheckService() {
        loanRepaymentUseCase.dueLoanRepaymentCheck();
    }

    @Scheduled(cron = "0 0/5 * ? * *") // runs by every 5 minutes
    @SchedulerLock(name = "LoanJob_runProcessApprovedLoans", lockAtMostForString = "PT6M")
    public void runProcessApprovedLoans() {
        try {
            approvalUseCase.processApprovedLoans();
        } catch (Exception ignored) {
        }
    }

    @Scheduled(cron = "0 0 7-20 ? * *") // runs every 1 hour from 7am to 8pm
    @SchedulerLock(name = "InvestmentMaturityUpdateJob_processInvestmentMaturityUpdate", lockAtMostForString = "PT30M")
    public void processInvestmentMaturityUpdate() {
        log.info("cron task processInvestmentMaturityUpdate");
        updateInvestmentMaturityUseCase.updateStatusForMaturedInvestment();
    }

}
