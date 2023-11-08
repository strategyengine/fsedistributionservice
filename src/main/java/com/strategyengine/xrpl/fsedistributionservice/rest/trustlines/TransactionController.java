package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.transactions.Transaction;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.XrplNetwork;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropSummaryService;
import com.strategyengine.xrpl.fsedistributionservice.service.AnalysisService;
import com.strategyengine.xrpl.fsedistributionservice.service.CurrencyHexService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Api(tags = "Transactions")
@RestController
public class TransactionController {

	
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
	protected CurrencyHexService currencyHexService;
	
	@Autowired
	protected XrplClientService xrplClientService;
	
	@ApiOperation(value = "Find the addresses that have been paid by this address")
	@RequestMapping(value = "/transactions/payments/{classicAddress}", method = RequestMethod.GET)
	public List<FseTransaction> getPaymentTransactions(
			@ApiParam(value = "Classic XRP address that sent the tokens. Example rnL2P...", required = true) @PathVariable("classicAddress") String classicAddress) {

		return transactionHistoryService.getTransactionBurns(classicAddress);
		

	}
	
	@ApiOperation(value = "Cancels all open offers for a seed")
	@RequestMapping(value = "/transactions/offers/cancel/{seed}", method = RequestMethod.POST)
	public List<SubmitResult<Transaction>> cancelOpenOffers(
			@ApiParam(value = "seed.", required = true) @PathVariable("seed") String seed,
			@ApiParam(value = "xrplNetwork", required = false) @RequestParam("xrplNetwork") XrplNetwork xrplNetwork) {

		try {
			
			return xrplClientService.cancelOpenOffers(seed, xrplNetwork!=null? xrplNetwork : XrplNetwork.XRPL_MAIN);
			
		} catch (Exception e) {
			log.error(e);
		}
		
		return null;
		

	}
	
	@ApiOperation(value = "Cancels all open offers for a seed")
	@RequestMapping(value = "/transactions/offers/cancel/{address}/{privKey}/{pubKey}", method = RequestMethod.POST)
	public List<SubmitResult<Transaction>> cancelOpenOffersKeys(
			@ApiParam(value = "address", required = true) @PathVariable("address") String address,
			@ApiParam(value = "privKey", required = true) @PathVariable("privKey") String privKey,
			@ApiParam(value = "pubKey", required = true) @PathVariable("pubKey") String pubKey,
			@ApiParam(value = "xrplNetwork", required = false) @RequestParam("xrplNetwork") XrplNetwork xrplNetwork) {

		try {
		
			return xrplClientService.cancelOpenOffers(address, privKey, pubKey, xrplNetwork!=null? xrplNetwork : XrplNetwork.XRPL_MAIN);
			
		} catch (Exception e) {
			log.error(e);
		}
		
		return null;
		

	}
	
}