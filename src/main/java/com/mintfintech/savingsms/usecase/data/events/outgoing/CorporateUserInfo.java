package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CorporateUserInfo {

    private String recipient;

    private String name;
}
