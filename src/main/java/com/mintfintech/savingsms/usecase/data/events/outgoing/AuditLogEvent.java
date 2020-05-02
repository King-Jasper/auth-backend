package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@Data
@Builder
public class AuditLogEvent {
    private String systemName;
    private String auditType;
    private String description;
    private String actorId;
    private String actorName;
    private String accountId;
    private String oldRecordPayload;
    private String newRecordPayload;
}
