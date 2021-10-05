package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropSummaryService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;
import com.strategyengine.xrpl.fsedistributionservice.service.TrustlineTriggerDropService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Api(tags = "XRPL Trustline endpoints")
@RestController
public class AnalysisController {

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

	@ApiOperation(value = "Create a summary of an airdrop between a time window")
	@RequestMapping(value = "/api/lineage/{classicAddress}}", method = RequestMethod.GET)
	public Set<String> getLineage(
			@ApiParam(value = "Classic XRP address that sent the tokens. Example rnL2P...", required = true) @PathVariable("classicAddress") String classicAddress) {

		List<FseTransaction> transactions = transactionHistoryService.getTransactions(classicAddress, Optional.empty(), 20000);
		
		return transactions.stream().filter(t -> "PAYMENT".equals(t.getTransactionType())).map(t -> t.getToAddress()).collect(Collectors.toSet());

	}


}