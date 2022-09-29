package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.strategyengine.xrpl.fsedistributionservice.FseConstants;
import com.strategyengine.xrpl.fsedistributionservice.model.AirdropStatus;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropSummaryService;
import com.strategyengine.xrpl.fsedistributionservice.service.AnalysisService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Api(tags = "Airdrop Metadata")
@RestController
public class AirdropMetadataController {

	
	@VisibleForTesting
	@Autowired
	protected AnalysisService analysisService;

	@VisibleForTesting
	@Autowired
	protected TransactionHistoryService transactionHistoryService;
	
	@VisibleForTesting
	@Autowired
	protected XrplService xrplService;

	@VisibleForTesting
	@Autowired
	protected AirdropSummaryService airdropSummaryService;
	
	@VisibleForTesting
	@Autowired
	protected TransactionService transactionService;
	
	
	private List<String> validApiKeys = ImmutableList.of("reticulogic");

	@ApiOperation(value = "Fetch Airdrops for an issuing address")
	@RequestMapping(value = "/api/airdrop/summary/{issuingAddress}", method = RequestMethod.GET)
	public List<AirdropStatus> airdrops(
			@ApiParam(value = "Classic XRP address that issued the tokens. Example rnL2P...", required = true) @PathVariable("issuingAddress") String issuingAddress) {
		
		return airdropSummaryService.getAirdrops(issuingAddress);
	}

	@ApiOperation(value = "Fetch details of an airdrop")
	@RequestMapping(value = "/api/airdrop/detail/{id}", method = RequestMethod.GET)
	public AirdropStatus airdropDetails(
			@ApiParam(value = "Airdrop request id", required = true) @PathVariable("id") Long paymentRequestId) {
		
		return airdropSummaryService.getAirdropDetails(paymentRequestId);
	}	

	@ApiOperation(value = "Approve an airdrop")
	@RequestMapping(value = "/api/airdrop/approve/{id}", method = RequestMethod.POST)
	public void airdropApprove(
			@ApiParam(value = "Airdrop request id", required = true) @PathVariable("id") Long paymentRequestId,
			@ApiParam(value = "Payment Details: Click Model under Data Type for details", required = true) @RequestBody FsePaymentRequest paymentRequest) {
		
		 xrplService.approveAirdrop(paymentRequestId, paymentRequest.getFromPrivateKey());
	}	
		
	
	@ApiOperation(value = "Fetch  incomplete airdrop")
	@RequestMapping(value = "/api/airdrop/active", method = RequestMethod.GET)
	public List<AirdropStatus> incompleteAirdrops() {
		
		return airdropSummaryService.getIncompleteAirdrops();
	}	
	
	@ApiOperation(value = "Fetch airdrops")
	@RequestMapping(value = "/api/airdrops", method = RequestMethod.GET)
	public List<AirdropStatus> getAirdrops() {
		
		return airdropSummaryService.getAirdrops();
	}	
	
	@ApiOperation(value = "Fetch transactions for a recipient")
	@RequestMapping(value = "/api/airdrop/transactions/{dropRecipientId}", method = RequestMethod.GET)
	public List<FseTransaction> getAirdrops(	
			@ApiParam(value = "Drop recipient id", required = true) @PathVariable("dropRecipientId") Long dropRecipientId) {
		
		return transactionService.getTransactionsForDropRecipientId(dropRecipientId);
	}	

}