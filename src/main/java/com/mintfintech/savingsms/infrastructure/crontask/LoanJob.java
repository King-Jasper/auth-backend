package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.LoanApprovalUseCase;
import com.mintfintech.savingsms.usecase.LoanRepaymentUseCase;
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
    @Scheduled(cron = "0 59 08 * * *", zone = "Africa/Lagos")
    public void sendNotificationForDuePayments() {
        log.info("Sending email notification to all customers with loan payment due");
        loanRepaymentUseCase.dispatchEmailToCustomersWithPaymentDueInTwoDays();
        loanRepaymentUseCase.dispatchEmailNotificationRepaymentOnDueDay();
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
