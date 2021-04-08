package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.LoanRepaymentUseCase;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Named;

@Slf4j
@Named
public class LoanJob {

    private final LoanRepaymentUseCase loanRepaymentUseCase;

    public LoanJob(LoanRepaymentUseCase loanRepaymentUseCase) {
        this.loanRepaymentUseCase = loanRepaymentUseCase;
    }

    @SchedulerLock(name = "LoanJob_sendNotificationForDuePayments", lockAtMostForString = "PT45M")
    @Scheduled(cron = "0 59 08 * * *", zone = "Africa/Lagos")
    public void sendNotificationForDuePayments() {
        log.info("Sending email notification to all customers with loan payment due");
        loanRepaymentUseCase.dispatchEmailToCustomersWithPaymentDueInTwoDays();
        loanRepaymentUseCase.dispatchEmailNotificationRepaymentOnDueDay();
    }

    @SchedulerLock(name = "LoanJob_runRepaymentPlanProcessingService", lockAtMostForString = "PT45M")
    @Scheduled(cron = "0 0 0/1 1/1 * ?") // runs every one hour.
    public void runRepaymentPlanProcessingService() {
        loanRepaymentUseCase.processPaymentOfDueRepayment();
    }

    @SchedulerLock(name = "LoanJob_runCheckDefaultedLoanPaymentService", lockAtMostForString = "PT45M")
    @Scheduled(cron = "0 50 23 1/1 * ?") // runs every day at 11:50pm.
    public void runCheckDefaultedLoanPaymentService() {
        loanRepaymentUseCase.checkDefaultedRepayment();
    }

}