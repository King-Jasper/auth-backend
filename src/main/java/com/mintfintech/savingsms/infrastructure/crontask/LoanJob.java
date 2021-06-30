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

    //
    @SchedulerLock(name = "LoanJob_loanRepaymentDueToday", lockAtMostForString = "PT45M")
    @Scheduled(cron = "0 00 23 1/1 * ?") // runs every day at 12:00pm.
    public void loanRepaymentDueToday() {
        loanRepaymentUseCase.loanRepaymentDueToday();
    }

    @Scheduled(cron = "0 0/5 * ? * *") // runs by every 5 minutes
    @SchedulerLock(name = "LoanJob_processApprovedLoans", lockAtMostForString = "PT6M")
    public void processApprovedLoans() {
        try {
            approvalUseCase.processApprovedLoans();
        } catch (Exception ignored) {
        }
    }
//0 0 9/3 ? * *  //0 0/30 0 ? * *
    @Scheduled(cron = "0 0 11/3 ? * *") // runs every 3 hour starting from 9am daily
    @SchedulerLock(name = "LoanJob_processDueLoanPendingDebit", lockAtMostForString = "PT30M")
    public void processDueLoanPendingDebit() {
        loanRepaymentUseCase.checkDueLoanPendingDebit();
    }

}
