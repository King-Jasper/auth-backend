package com.mintfintech.savingsms.domain.models.reports;

import lombok.Data;
/**
 * Created by jnwanya on
 * Mon, 13 Sep, 2021
 */
@Data
public class ReferralRewardStat {
    private boolean processed;
    private long count;
    public ReferralRewardStat(boolean processed, long count) {
        this.processed = processed;
        this.count = count;
    }
}
