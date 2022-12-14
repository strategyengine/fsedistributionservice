package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.PaymentsChange;
import com.strategyengine.xrpl.fsedistributionservice.model.RetryPaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
import com.strategyengine.xrpl.fsedistributionservice.service.ConfigService;
import com.strategyengine.xrpl.fsedistributionservice.service.PaymentsChangeService;
import com.strategyengine.xrpl.fsedistributionservice.service.RetryFailedPaymentsService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;
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
	protected ConfigService configService;

	@VisibleForTesting
	@Autowired
	protected PaymentsChangeService paymentsChangeService;


	@VisibleForTesting
	@Autowired
	protected RetryFailedPaymentsService retryFailedPaymentsService;
	
	@ApiOperation(value = "Distributes tokens to a set of recipient addresses")
	@RequestMapping(value = "/api/payment", method = RequestMethod.POST)
	public ResponseEntity<Void> payment(
			@ApiParam(value = "Payment Details: Click Model under Data Type for details", required = true) @RequestBody FsePaymentRequest paymentRequest) {

		if(!configService.isAirdropEnabled()) {
			throw new BadRequestException("Airdrops currently disabled");
		}
		log.info(
				"api/payment: specific addresses - fromClassicAddress:{} fromSigningPublicKey:{} amount:{} issuerClassicAddress:{} currency:{} agreeFee:{} addressesToPay:{}",
				paymentRequest.getFromClassicAddress(), paymentRequest.getFromSigningPublicKey(),
				paymentRequest.getAmount(), paymentRequest.getTrustlineIssuerClassicAddress(),
				paymentRequest.getCurrencyName(), paymentRequest.isAgreeFee(), paymentRequest.getToClassicAddresses());

		if ("String".equals(paymentRequest.getDestinationTag())
				|| ObjectUtils.isEmpty(paymentRequest.getDestinationTag())) {
			paymentRequest.setDestinationTag(null);
		}
		
		xrplService.sendFsePayment(paymentRequest);
		return ResponseEntity.ok().build();
	}

	@ApiOperation(value = "Distributes tokens to trustline holders.  Airdrop")
	@RequestMapping(value = "/api/payment/trustlines", method = RequestMethod.POST)
	public ResponseEntity<Void> paymentTrustlines(
			@ApiParam(value = "Payment Details: Click Model under Data Type for details", required = true) @RequestBody FsePaymentTrustlinesRequest paymentRequest) {
		
		if(!configService.isAirdropEnabled()) {
			throw new BadRequestException("Airdrops currently disabled");
		}
		
		// DO NOT LOG THE PRIVATE KEY!!
		log.info(
				"payment/trustlines: {}",
				paymentRequest);

		xrplService.sendFsePaymentToTrustlines(paymentRequest, null);

		return ResponseEntity.ok().build();
	}

	@ApiOperation(value = "Cancels a running Airdrop")
	@RequestMapping(value = "/api/payment/trustlines/cancel", method = RequestMethod.POST)
	public ResponseEntity<FsePaymentRequest> paymentTrustlines(
			@ApiParam(value = "Payment Details: Click Model under Data Type for details", required = true) @RequestBody FsePaymentRequest paymentRequest,
			@ApiParam(value = "If true, will cancel scheduled jobs as well", required = true) @RequestParam(value="cancelScheduled") Boolean cancelScheduled) {
		
		//only the private key and issuing address are populated
		
		// DO NOT LOG THE PRIVATE KEY!!
		log.info(
				"payment/trustlines/cancel");

		FsePaymentRequest result = xrplService.cancelJob(paymentRequest.getFromPrivateKey(), paymentRequest.getTrustlineIssuerClassicAddress(), cancelScheduled);
		
		return ResponseEntity.of(Optional.ofNullable(result));

	}
	
	@ApiOperation(value = "Distributes tokens to a set of recipient addresses")
	@RequestMapping(value = "/api/payment/retryfailed", method = RequestMethod.POST)
	public ResponseEntity<FsePaymentResult> retryFailedPayment(
			@ApiParam(value = "Payment Details: Click Model under Data Type for details", required = true) @RequestBody RetryPaymentRequest retryPaymentRequest) {

		if(!configService.isAirdropEnabled()) {
			throw new BadRequestException("Airdrops currently disabled");
		}
		log.info(
				"payment/trustlines/retryfailed: dropRequestId {}",
				retryPaymentRequest);

		
		FsePaymentResult r = retryFailedPaymentsService.retryFailedPayments(retryPaymentRequest);
		return ResponseEntity.ok(r);
	}
	
	@ApiOperation(value = "Change amounts to pay to specific addresses")
	@RequestMapping(value = "/api/payment/change", method = RequestMethod.POST)
	public void changePaymentAmounts(
			@ApiParam(value = "Payments Change", required = true) @RequestBody PaymentsChange paymentsChange) {

			paymentsChangeService.updatePaymentAmounts(paymentsChange);
	}
	
	@ApiOperation(value = "Change amounts to pay to specific addresses")
	@RequestMapping(value = "/api/payment/delete/{dropRecipientId}", method = RequestMethod.POST)
	public void deletePayment(
			@ApiParam(value = "Drop recipient id", required = true) @PathVariable("dropRecipientId") Long dropRecipientId,
			@ApiParam(value = "Payments Delete", required = true) @RequestBody PaymentsChange paymentsChange) {

			paymentsChangeService.removeRecipient(dropRecipientId, paymentsChange.getPrivateKey());
	}

}