package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.strategyengine.xrpl.fsedistributionservice.model.AirdropSummary;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropSummaryService;
import com.strategyengine.xrpl.fsedistributionservice.service.AnalysisService;
import com.strategyengine.xrpl.fsedistributionservice.service.BlacklistService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Api(tags = "Trustline analysis")
@RestController
public class AnalysisController {

	
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
	protected BlacklistService blacklistService;
	
	@ApiOperation(value = "Find the addresses that have been paid by this address")
	@RequestMapping(value = "/analysis/paidaddresses/{classicAddress}", method = RequestMethod.GET)
	public Set<String> getLineage(
			@ApiParam(value = "Classic XRP address that sent the tokens. Example rnL2P...", required = true) @PathVariable("classicAddress") String classicAddress) {

		return analysisService.getPaidAddresses(classicAddress);

	}
	
	
	
	@ApiOperation(value = "Get the Trustlines for an XRP address")
	@RequestMapping(value = "/api/trustlines/{classicAddress}", method = RequestMethod.GET)
	public List<FseTrustLine> trustLines(
			@ApiParam(value = "Classic XRP address. Example rnL2P...", required = true) @PathVariable("classicAddress") String classicAddress,
			@ApiParam(value = "OPTOINAL - Enter a currency to include or exclude from the response. DEFAULT is include", required = false) @RequestParam(value = "filterCurrency", required = false) String filterCurrency,
			@ApiParam(value = "OPTIONAL - true will only return results with the currency parameter, false will return all results not having the currency param", required = false) @RequestParam(value = "includeFilter", required = false) Boolean includeFilter,
		    @ApiParam(value = "OPTIONAL - true will sort results by poor to rich, false will sort by newest added to oldest", required = false) @RequestParam(value = "sortByRich", required = false) Boolean sortByRich) {

			
		return xrplService.getTrustLines(classicAddress, Optional.ofNullable(filterCurrency),
				includeFilter == null ? true : includeFilter, 
				sortByRich == null ? true: sortByRich);
	}

	@ApiOperation(value = "Create a summary of an airdrop between a time window")
	@RequestMapping(value = "/api/airdrop/summary/{classicAddress}/{issuingAddress}/{currency}", method = RequestMethod.GET)
	public AirdropSummary validateAirdrop(
			@ApiParam(value = "Classic XRP address that sent the tokens. Example rnL2P...", required = true) @PathVariable("classicAddress") String classicAddress,
			@ApiParam(value = "Classic XRP address that issued the tokens. Example rnL2P...", required = true) @PathVariable("issuingAddress") String issuingAddress,
			@ApiParam(value = "Currency code.", required = true) @PathVariable("currency") String currency,
			@ApiParam(value = "startTime in GMT - FORMAT  yyyy-MM-dd HH:mm:ss 2000-10-31 01:30:00", required = true) @RequestParam("startTime") LocalDateTime startTime,
			@ApiParam(value = "endTime in GMT - FORMAT  yyyy-MM-dd HH:mm:ss 2000-10-31 01:30:00", required = true) @RequestParam("stopTime") LocalDateTime stopTime,
			@ApiParam(value = "Amount that was dropped to each address", required = true) @RequestParam("dropAmount") String dropAmount) {

		Date start = Date.from(startTime.atZone(ZoneId.of("UTC")).toInstant());
		Date stop = Date.from(stopTime.atZone(ZoneId.of("UTC")).toInstant());
		
		return airdropSummaryService.airdropSummary(classicAddress, issuingAddress, currency, start, stop,
				new BigDecimal(dropAmount));
	}
	
	@ApiOperation(value = "Get the transactions for an XRP address")
	@RequestMapping(value = "/api/transactions/{classicAddress}", method = RequestMethod.GET)
	public List<FseTransaction> transactions(
			@ApiParam(value = "Classic XRP address. Example rnL2P...", required = true) @PathVariable("classicAddress") String classicAddress,
			@ApiParam(value = "Max ledger index", required = false) @RequestParam(value = "maxLedgerIndex", required = false) Long maxLedgerIndex,
			@ApiParam(value = "Max results to return - DEFAULT 1000  MAX 100k", required = false) @RequestParam(value = "limit", required = false) Integer limit) {
		if (limit != null && limit > 100000) {
			throw new BadRequestException("Max limit of 100k exceeded.");

		}
		return transactionHistoryService.getTransactions(classicAddress, Optional.ofNullable(maxLedgerIndex),
				limit == null ? 1000 : limit);
	}

	@ApiOperation(value = "Get the details for an XRP address")
	@RequestMapping(value = "/api/accountinfo", method = RequestMethod.GET)
	public List<FseAccount> accountInfo(
			@ApiParam(value = "Classic XRP address. Example rnL2P...", required = true) @RequestParam(value="classicAddress", required=true) List<String> classicAddresses) {
		return xrplService.getAccountInfo(classicAddresses);
	}

	
	@ApiOperation(value = "Get the blacklisted addresses")
	@RequestMapping(value = "/api/blacklisted", method = RequestMethod.GET)
	public Map<String, List<String>> blacklisted() {
		return blacklistService.getBlackListedFromAnalysis();
	}
	
	
	//NOT ready yet.  activation address not returning consinstently from xrplclient
	@ApiOperation(value = "Find addresses with trustlines that activate many child addresses with the same trustline")
	@RequestMapping(value = "/analysis/activations/{issuingAddress}/{currencyName}", method = RequestMethod.GET)
	public Map<String, List<String>> getActivations(
			@ApiParam(value = "Classic XRP address that issued the tokens. Example rnL2P...", required = true) @PathVariable("issuingAddress") String issuingAddress,
			@ApiParam(value = "Currency name Example FSE", required = true) @PathVariable("currencyName") String currencyName,
			@ApiParam(value = "OPTIONAL Minimum activations to return a parent. Default is 20 For example 10, will only return parent addresses that have activated 10 or more addresses containing this trustline", required = true) @RequestParam(value="minActivations", required=false, defaultValue = "20") int minActivations) {
		
		//TODO uncomment this to be able to run an analysis on addresses to blacklist
		return ImmutableMap.of("This is processor intesive so not allowed on the server.  See if @_Bear_Bull will run it for you or run it on your local.  (To run locally, comment line 147 and uncomment line 144 in AnalysisController.java)", Lists.newArrayList());
		
		//TODO remove comments here to run an analysis on addresses to blacklist
//		return analysisService.getActivations(issuingAddress, currencyName, minActivations);
	}


}