package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class CorporateInvestmentEmailEvent {

    private List<CorporateUserInfo> userInfo;
}
