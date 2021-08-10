package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import lombok.Data;

/**
 * Created by jnwanya on
 * Mon, 09 Aug, 2021
 */
@Data
public class LoanManager {
    private String reviewerName;
    private String reviewerUserId;
    private boolean loanOfficer;
    private boolean riskOfficer;
    private boolean financeOfficer;
    private boolean businessManager;


    public static LoanManager getManager(AuthenticatedUser authenticatedUser) {
        LoanManager loanManager = new LoanManager();
        loanManager.setReviewerUserId(authenticatedUser.getUserId());
        loanManager.setReviewerName(authenticatedUser.getName());
        loanManager.setLoanOfficer(authenticatedUser.getAuthorities().stream().anyMatch(data -> data.getAuthority().equalsIgnoreCase("29")));
        loanManager.setRiskOfficer(authenticatedUser.getAuthorities().stream().anyMatch(data -> data.getAuthority().equalsIgnoreCase("30")));
        loanManager.setFinanceOfficer(authenticatedUser.getAuthorities().stream().anyMatch(data -> data.getAuthority().equalsIgnoreCase("31")));
        loanManager.setBusinessManager(authenticatedUser.getAuthorities().stream().anyMatch(data -> data.getAuthority().equalsIgnoreCase("32")));
        return loanManager;
    }
}
