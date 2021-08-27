package com.strategyengine.xrpl.fsedistributionservice.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesMinTriggeredRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.service.TrustlineTriggerDropService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;


@RestController
public class XrplController {

	@VisibleForTesting
	@Autowired
	protected XrplService xrplService;
	
	@VisibleForTesting
	@Autowired
	protected TrustlineTriggerDropService trustlineTriggerDropService;
	
	
	@RequestMapping(value = "/api/trustlines/{classicAddress}", method = RequestMethod.GET)
	public List<FseTrustLine> trustLines(@PathVariable("classicAddress") String classicAddress) {
		return xrplService.getTrustLines(classicAddress);
	}

	@RequestMapping(value = "/api/accountinfo/{classicAddress}", method = RequestMethod.GET)
	public FseAccount accountInfo(@PathVariable("classicAddress") String classicAddress) {
		return xrplService.getAccountInfo(classicAddress);
	}

	@RequestMapping(value = "/api/payment", method = RequestMethod.POST)
	public FsePaymentResult payment(@RequestBody FsePaymentRequest paymentRequest) {
		return xrplService.sendFsePayment(paymentRequest);
	}
	
	@RequestMapping(value = "/api/payment/trustlines", method = RequestMethod.POST)
	public List<FsePaymentResult> paymentTrustlines(@RequestBody FsePaymentTrustlinesRequest paymentRequest) {
		return xrplService.sendFsePaymentToTrustlines(paymentRequest);
	}
	
	@RequestMapping(value = "/api/payment/trustlines/min/airdrop", method = RequestMethod.POST)
	public void paymentTrustlinesMinAirdrop(@RequestBody FsePaymentTrustlinesMinTriggeredRequest paymentRequest) {
		//this will block a http accept thread.   Add a thread pool to call this if you are going to call a lot of these.
		trustlineTriggerDropService.triggerAirdropAfterMinTrustlines(paymentRequest);
	}
	
}