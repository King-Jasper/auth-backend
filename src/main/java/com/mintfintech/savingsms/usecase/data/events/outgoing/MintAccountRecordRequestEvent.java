package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created by jnwanya on
 * Sat, 25 Apr, 2020
 */
@Builder
@Data
public class MintAccountRecordRequestEvent {
    private List<String> accountIds;
    private String topicNameSuffix;
}
