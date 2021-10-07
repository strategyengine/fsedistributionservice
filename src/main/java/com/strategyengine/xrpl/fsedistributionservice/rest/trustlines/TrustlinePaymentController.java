package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResults;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropSummaryService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;
import com.strategyengine.xrpl.fsedistributionservice.service.TrustlineTriggerDropService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Api(tags = "Airdrop & payments endpoints")
@RestController
public class TrustlinePaymentController {

	@VisibleForTesting
	@Autowired
	protected XrplService xrplService;
	
	@VisibleForTesting
	@Autowired
	protected TransactionHistoryService transactionHistoryService;

	@VisibleForTesting
	@Autowired
	protected TrustlineTriggerDropService trustlineTriggerDropService;

	@VisibleForTesting
	@Autowired
	protected AirdropSummaryService airdropSummaryService;
		


	@ApiOperation(value = "Distributes tokens to a set of recipient addresses")
	@RequestMapping(value = "/api/payment", method = RequestMethod.POST)
	public FsePaymentResults payment(
			@ApiParam(value = "Payment Details: Click Model under Data Type for details", required = true) @RequestBody FsePaymentRequest paymentRequest) {
		if("String".equals(paymentRequest.getDestinationTag())||StringUtils.isEmpty(paymentRequest.getDestinationTag())) {
			paymentRequest.setDestinationTag(null);
		}
		Date start = new Date();
		List<FsePaymentResult> results = xrplService.sendFsePayment(paymentRequest);
		return FsePaymentResults.builder().end(new Date()).start(start).results(results).transactionCount(results.size()).build();
	}


	@ApiOperation(value = "Distributes tokens to trustline holders.  Airdrop")
	@RequestMapping(value = "/api/payment/trustlines", method = RequestMethod.POST)
	public FsePaymentResults paymentTrustlines(
			@ApiParam(value = "Payment Details: Click Model under Data Type for details", required = true) @RequestBody FsePaymentTrustlinesRequest paymentRequest) {
		// DO NOT LOG THE PRIVATE KEY!!
		log.info(
				"payment/trustlines: fromClassicAddress:{} fromSigningPublicKey:{} amount:{} issuerClassicAddress:{} currency:{} maxTrustlines:{} agreeFee:{} newTrustlinesOnly:{}"
					, paymentRequest.getFromClassicAddress(),
				paymentRequest.getFromSigningPublicKey(), paymentRequest.getAmount(),
				paymentRequest.getTrustlineIssuerClassicAddress(), paymentRequest.getCurrencyName(),
				paymentRequest.getMaximumTrustlines(), paymentRequest.isAgreeFee(), paymentRequest.isNewTrustlinesOnly()
				);
		return xrplService.sendFsePaymentToTrustlines(paymentRequest);
	}

	
	//commenting so it's not used on the server
//	@ApiOperation(value = "Distributes tokens to trustline holders only after a minimum number of trustlines have been created.  Thread will check on number of trustlines in 10 minute intervals until minimum number is reached.")
//	@RequestMapping(value = "/api/payment/trustlines/min/airdrop", method = RequestMethod.POST)
//	public void paymentTrustlinesMinAirdrop(
//			@ApiParam(value = "Payment Details: Click Model under Data Type for details", required = true) @RequestBody FsePaymentTrustlinesMinTriggeredRequest paymentRequest) {
//
//		// DO NOT LOG THE PRIVATE KEY!!
//		log.info(
//				"payment/trustlines/min/airdrop: fromClassicAddress:{} fromSigningPublicKey:{} amount:{} issuerClassicAddress:{}"
//						, paymentRequest.getTrustlinePaymentRequest().getFromClassicAddress(),
//				paymentRequest.getTrustlinePaymentRequest().getFromSigningPublicKey(),
//				paymentRequest.getTrustlinePaymentRequest().getAmount(),
//				paymentRequest.getTrustlinePaymentRequest().getTrustlineIssuerClassicAddress());
//
//		// this will block a http accept thread. Add a thread pool to call this if you
//		// are going to call a lot of these.
//		trustlineTriggerDropService.triggerAirdropAfterMinTrustlines(paymentRequest);
//	}

}