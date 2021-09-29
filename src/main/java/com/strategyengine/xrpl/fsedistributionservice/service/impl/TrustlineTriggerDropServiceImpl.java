package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesMinTriggeredRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.service.TrustlineTriggerDropService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class TrustlineTriggerDropServiceImpl implements TrustlineTriggerDropService {

	@VisibleForTesting
	@Autowired
	protected XrplService xrplService;
	
	@VisibleForTesting
	protected static long MILLIS_SLEEP = 1000*60*10;

	ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

	/**
	 * After a minimum number of trustlines are reached, the airdrop will begin
	 */
	@Override
	public void triggerAirdropAfterMinTrustlines(FsePaymentTrustlinesMinTriggeredRequest req) {

		executorService
				.schedule(
						() -> triggerDrop(xrplService.getTrustLines(
								req.getTrustlinePaymentRequest().getTrustlineIssuerClassicAddress(), false), req),
						MILLIS_SLEEP, TimeUnit.MILLISECONDS);
	}

	@VisibleForTesting
	protected void triggerDrop(List<FseTrustLine> trustLines, FsePaymentTrustlinesMinTriggeredRequest req) {

		if (trustLines.size() >= req.getMinTrustLinesTriggerValue()) {
			log.info("We've got enough trustlines to trigger!!   AIRDROP IT!");
			xrplService.sendFsePaymentToTrustlines(req.getTrustlinePaymentRequest());
		} else {
			log.info("Not enought trustlines to send the triggered airdrop.  Total trustlines" + trustLines.size());
			// check if there are enough trustlines to see if we can AIRDROP it.
			executorService.schedule(
					() -> triggerDrop(xrplService
							.getTrustLines(req.getTrustlinePaymentRequest().getTrustlineIssuerClassicAddress(), false), req),
					MILLIS_SLEEP, TimeUnit.MILLISECONDS);
		}

	}
}
