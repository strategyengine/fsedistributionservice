package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.List;

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
	
	
	/**
	 * After a minimum number of trustlines are reached, the airdrop will begin
	 */
	@Override
	public void triggerAirdropAfterMinTrustlines(FsePaymentTrustlinesMinTriggeredRequest req) {
	
		int TEN_MINUTES_MILLIS = 1000*60*10;
		triggerDrop(xrplService.getTrustLines(req.getTrustlinePaymentRequest().getTrustlineIssuerClassicAddress()), req, TEN_MINUTES_MILLIS);
	}

	@VisibleForTesting
	protected void triggerDrop(List<FseTrustLine> trustLines, FsePaymentTrustlinesMinTriggeredRequest req, int sleepWindow) {

		if(trustLines.size() >= req.getMinTrustLinesTriggerValue()) {
			log.info("We've got enough trustlines to trigger!!   AIRDROP IT!");
			xrplService.sendFsePaymentToTrustlines(req.getTrustlinePaymentRequest());
		} else {
			log.info("Not enought trustlines to send the triggered airdrop.  Total trustlines" + trustLines.size());
			try {
				Thread.sleep(sleepWindow);
			}catch(Exception e) {
				log.error("Failed to wait my turn for triggered drop!", e);
			}
			//check if there are enough trustlines to see if we can AIRDROP it.
			triggerDrop(xrplService.getTrustLines(req.getTrustlinePaymentRequest().getTrustlineIssuerClassicAddress()), req, sleepWindow);
		}
		
		
		
	}
}
