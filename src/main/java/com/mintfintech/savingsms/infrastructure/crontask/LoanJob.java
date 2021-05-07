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
    @Scheduled(cron = "0 15 12 1/1 * ?", zone = "Africa/Lagos") // runs every day at 00:01am.
    public void sendNotificationForDuePayments() {
        log.info("Sending email notification to all customers with loan payment due");
        loanRepaymentUseCase.dispatchEmailToCustomersWithPaymentDueInTwoDays();
    }

    @SchedulerLock(name = "LoanJob_runCheckDefaultedLoanPaymentService", lockAtMostForString = "PT45M")
    @Scheduled(cron = "0 50 23 1/1 * ?") // runs every day at 11:50pm.
    public void runCheckDefaultedLoanPaymentService() {
        loanRepaymentUseCase.checkDefaultedRepayment();
    }

    @Scheduled(cron = "0 0/5 * ? * *") // runs by every 5 minutes
    @SchedulerLock(name = "LoanJob_runApprovedLoanPendingDisbursement", lockAtMostForString = "PT6M")
    public void runApprovedLoanPendingDisbursement() {
        try {
            approvalUseCase.processMintToSuspenseAccount();
            Thread.sleep(500);
            approvalUseCase.processInterestToSuspenseAccount();
            Thread.sleep(500);
            approvalUseCase.processSuspenseAccountToCustomer();
        } catch (Exception ignored) {
        }
    }

    @Scheduled(cron = "0 0/5 * ? * *") // runs by every 5 minutes
    @SchedulerLock(name = "LoanJob_runPendingApprovedRepayment", lockAtMostForString = "PT6M")
    public void runPendingApprovedRepayment() {
        try {
            loanRepaymentUseCase.processLoanRecoverySuspenseAccountToMintLoanAccount();
            Thread.sleep(500);
            loanRepaymentUseCase.processInterestIncomeSuspenseAccountToInterestIncomeAccount();
        } catch (Exception ignored) {
        }
    }


}
