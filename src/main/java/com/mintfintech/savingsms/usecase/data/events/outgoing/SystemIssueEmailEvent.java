package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Fri, 01 May, 2020
 */
@Builder
@Data
public class SystemIssueEmailEvent {
    private String recipient;
    private String title;
    private String detail;
}
