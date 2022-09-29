package com.strategyengine.xrpl.fsedistributionservice.service;

import java.util.List;

import com.strategyengine.xrpl.fsedistributionservice.model.AirdropStatus;

public interface AirdropSummaryService {

	List<AirdropStatus> getAirdrops(String issuingAddress);

	AirdropStatus getAirdropDetails(Long paymentRequestId);


	List<AirdropStatus> getAirdrops();

	List<AirdropStatus> getIncompleteAirdrops();

}
