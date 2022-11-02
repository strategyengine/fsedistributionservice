package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.strategyengine.xrpl.fsedistributionservice.model.DropRecipientTransactions;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseSort;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropRecipientRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropSummaryService;
import com.strategyengine.xrpl.fsedistributionservice.service.AnalysisService;
import com.strategyengine.xrpl.fsedistributionservice.service.BlacklistService;
import com.strategyengine.xrpl.fsedistributionservice.service.CurrencyHexService;
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
	
	@VisibleForTesting
	@Autowired
	protected CurrencyHexService currencyHexService;
	
	@Autowired
	protected DropRecipientRepo dropRecipientRepo;

	@Autowired
	protected PaymentRequestRepo paymentRequestRepo;
	
	
	private List<String> validApiKeys = ImmutableList.of("reticulogic");
	
	@ApiOperation(value = "Find the addresses that have been paid by this address")
	@RequestMapping(value = "/analysis/paidaddresses/{classicAddress}", method = RequestMethod.GET)
	public Map<String, Set<String>> getLineage(
			@ApiParam(value = "Classic XRP address that sent the tokens. Example rnL2P...", required = true) @PathVariable("classicAddress") String classicAddress) {

		Map<String, Set<String>> payments = new HashMap<String, Set<String>>();
		payments.put("PAID_TO",analysisService.getPaidAddresses(classicAddress));
		//payments.put("PAID_BY", this.transactions(classicAddress, null, 20000).stream()
			//	.filter(t -> "PAYMENT".equals(t.getTransactionType()))
			//	.map(t -> t.getFromAddress()).collect(Collectors.toSet()));

		return payments;

	}
	
	@ApiOperation(value = "Find all addresses that overlap between 2 different currencies")
	@RequestMapping(value = "/analysis/currency/addressoverlap/{issueAddress1}/{currency1}/{issueAddress2}/{currency2}", method = RequestMethod.GET)
	public List<FseTrustLine> getLineage(
			@ApiParam(value = "Issuing address. Example rnL2P...", required = true) @PathVariable("issueAddress1") String issueAddress1,
			@ApiParam(value = "Currency code", required = true) @PathVariable("currency1") String currency1,
			@ApiParam(value = "Issuing address. Example rnL2P...", required = true) @PathVariable("issueAddress2") String issueAddress2,
			@ApiParam(value = "Currency code.", required = true) @PathVariable("currency2") String currency2) {

		String currency1Fixed = currencyHexService.fixCurrencyCode(currency1);
		String currency2Fixed = currencyHexService.fixCurrencyCode(currency2);

		
		List<FseTrustLine> tl1 = xrplService.getTrustLines(issueAddress1, Optional.of(currency1), Optional.of(currency1Fixed), true, FseSort.OLD);
		List<FseTrustLine> tl2 = xrplService.getTrustLines(issueAddress2, Optional.of(currency2), Optional.of(currency2Fixed), true, FseSort.OLD);
		
		if(tl1.size() < tl2.size()) {
			return tl1.stream().filter(t -> contains(tl2, t)).collect(Collectors.toList());
		}
		return tl2.stream().filter(t -> contains(tl1, t))
				.collect(Collectors.toList());
	}	

	
	private boolean contains(List<FseTrustLine> tl2, FseTrustLine t) {
		return tl2.stream().anyMatch(t2 -> t2.getClassicAddress().equals(t.getClassicAddress()));
	}

	@ApiOperation(value = "Get the Trustlines for an XRP address")
	@RequestMapping(value = "/api/trustlines/{classicAddress}", method = RequestMethod.GET)
	public List<FseTrustLine> trustLines(
			@ApiParam(value = "Classic XRP address. Example rnL2P...", required = true) @PathVariable("classicAddress") String classicAddress,
			@ApiParam(value = "OPTOINAL - Enter a currency to include or exclude from the response. DEFAULT is include", required = false) @RequestParam(value = "filterCurrency", required = false) String filterCurrency,
			@ApiParam(value = "OPTIONAL - true will only return results with the currency parameter, false will return all results not having the currency param", required = false) @RequestParam(value = "includeFilter", required = false) Boolean includeFilter,
		    @ApiParam(value = "OPTIONAL - true will sort results by poor to rich, false will sort by newest added to oldest", required = false) @RequestParam(value = "sortByRich", required = false) Boolean sortByRich) {

			String processCurrency = currencyHexService.fixCurrencyCode(filterCurrency);
		return xrplService.getTrustLines(classicAddress, Optional.ofNullable(processCurrency), Optional.ofNullable(filterCurrency),
				includeFilter == null ? true : includeFilter, 
				sortByRich == null ? FseSort.OLD: FseSort.RICH);
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
				limit == null ? 1000 : limit, Optional.empty());
	}

	@ApiOperation(value = "Get the XRPL transactions for an Airdrop")
	@RequestMapping(value = "/api/transactions/drop/{dropRequestId}", method = RequestMethod.GET)
	public List<DropRecipientTransactions> transactionsForVerification(
			@ApiParam(value = "Drop Request ID", required = false) @PathVariable(value = "dropRequestId") Long dropRequestId) {

		
		return analysisService.getTransactionsForPaymentRequestId(dropRequestId);

			
	}
	


	@ApiOperation(value = "Get the details for an XRP address")
	@RequestMapping(value = "/api/accountinfo", method = RequestMethod.GET)
	public List<FseAccount> accountInfo(
			@ApiParam(value = "Classic XRP address. Example rnL2P...", required = true) @RequestParam(value="classicAddress", required=true) List<String> classicAddresses,
			@ApiParam(value = "Include trustlines", required = false) @RequestParam(value="includeTrust", required=true) Boolean includeTrust) {
		
		return xrplService.getAccountInfo(classicAddresses, includeTrust==null? true : includeTrust);
	}

	@ApiOperation(value = "Get the blacklisted addresses")
	@RequestMapping(value = "/api/blacklisted/{address}", method = RequestMethod.GET)
	public ResponseEntity<String> blacklisted(
			@ApiParam(value = "address", required = true)
			@PathVariable("address") String address) {
		
		return ResponseEntity.ok("If you didn't see it in the results, then almost certainly true.");
//		return ResponseEntity.ok(blacklistService.isBlackListedAddress(address));
		
	}
	
	//NOT ready yet.  activation address not returning consinstently from xrplclient
	@ApiOperation(value = "Find addresses with trustlines that activate many child addresses with the same trustline")
	@RequestMapping(value = "/analysis/activations/{issuingAddress}/{currencyName}/{apikey}", method = RequestMethod.GET)
	public Map<String, List<String>> getActivations(
			@ApiParam(value = "Classic XRP address that issued the tokens. Example rnL2P...", required = true) @PathVariable("issuingAddress") String issuingAddress,
			@ApiParam(value = "Currency name Example FSE", required = true) @PathVariable("currencyName") String currencyName,
			@ApiParam(value = "ApiKey to run this job", required = true) @PathVariable("apiKey") String apiKey,
			@ApiParam(value = "OPTIONAL Minimum activations to return a parent. Default is 50 For example 10, will only return parent addresses that have activated 10 or more addresses containing this trustline", required = true) @RequestParam(value="minActivations", required=false, defaultValue = "50") int minActivations) {
		
		if(!"reticulogic".equals(apiKey)) {
			return ImmutableMap.of("This is processor intesive so not allowed on the server.  See if @_Bear_Bull will run it for you or run it on your local.  (To run locally, comment line 147 and uncomment line 144 in AnalysisController.java)", Lists.newArrayList());
		}
		
		return analysisService.getActivations(issuingAddress, currencyName, minActivations);
	}

	
	@ApiOperation(value = "Find the addresses that have been verified for an issuer")
	@RequestMapping(value = "/analysis/verified/{issueAddress}/{currency}", method = RequestMethod.GET)
	public Map<String, List<FseTrustLine>> getLineage(
			@ApiParam(value = "Issue XRP address that sent the tokens. Example rnL2P...", required = true) @PathVariable("issueAddress") String issueAddress,
			@ApiParam(value = "Currency", required = true) @PathVariable("currency") String currency) {

		log.info("analysis/verified/{}/{} running verified analysis check ", issueAddress, currency);
		return analysisService.getVerifiedForIssuer(issueAddress, currency);

	}
	
	@ApiOperation(value = "Find the trustlines that would be paid by a cross currency drop")
	@RequestMapping(value = "/analysis/crosscurrency/trustlines", method = RequestMethod.POST)
	public List<FseTrustLine> fetchTrustlinesForCrossCurrencyDrop(
			@ApiParam(value = "Payment Details: Click Model under Data Type for details", required = true) @RequestBody FsePaymentTrustlinesRequest paymentRequest) {

		log.info("/analysis/crosscurrency/trustlines " + paymentRequest);
		return xrplService.fetchAllTrustlines(paymentRequest);

	}
	
	
	


}