package com.strategyengine.xrpl.fsedistributionservice.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.strategyengine.xrpl.fsedistributionservice.model.AirdropSummary;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;

import lombok.NonNull;

public interface AirdropSummaryService {

	AirdropSummary airdropSummary(String classicAddress, String issuingAddress, String currency, Date startTime,
			Date stopTime, BigDecimal dropAmount);

	AirdropSummary createSummary(@NonNull String fromClassicAddress, @NonNull String trustlineIssuerClassicAddress,
			@NonNull String currencyName, List<FseTrustLine> eligibleTrustLines, Date airDropStartTime,
			Date airDropEndTime, @NonNull BigDecimal amount);

}
