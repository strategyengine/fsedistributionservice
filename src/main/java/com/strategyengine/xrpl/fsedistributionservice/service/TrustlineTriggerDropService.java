package com.strategyengine.xrpl.fsedistributionservice.service;

import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesMinTriggeredRequest;

public interface TrustlineTriggerDropService {

	/**
	 * After a minimum number of trustlines are reached, the airdrop will begin
	 */
	void triggerAirdropAfterMinTrustlines(FsePaymentTrustlinesMinTriggeredRequest req);

}
