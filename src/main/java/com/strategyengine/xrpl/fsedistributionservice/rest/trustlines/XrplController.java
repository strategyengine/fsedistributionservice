package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xrpl.xrpl4j.keypairs.DefaultKeyPairService;
import org.xrpl.xrpl4j.wallet.DefaultWalletFactory;
import org.xrpl.xrpl4j.wallet.Wallet;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.model.AirdropSummary;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesMinTriggeredRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.model.FseWallet;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
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
public class XrplController {

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
	
	@ApiOperation(value = "Get the Trustlines for an XRP address sorted by poorest.  Rich list at the bottom")
	@RequestMapping(value = "/api/trustlines/{classicAddress}", method = RequestMethod.GET)
	public List<FseTrustLine> trustLines(
			@ApiParam(value = "Classic XRP address. Example rnL2P...", required = true) @PathVariable("classicAddress") String classicAddress,
			@ApiParam(value = "OPTOINAL - Enter a currency to include or exclude from the response. DEFAULT is include", required = false) @RequestParam(value = "filterCurrency", required = false) String filterCurrency,
			@ApiParam(value = "OPTIONAL - true will only return results with the currency parameter, false will return all results not having the currency param", required = false) @RequestParam(value = "includeFilter", required = false) Boolean includeFilter) {

		return xrplService.getTrustLines(classicAddress, Optional.ofNullable(filterCurrency),
				includeFilter == null ? true : includeFilter);
	}

	@ApiOperation(value = "Generates some XRP wallets")
	@RequestMapping(value = "/api/walletgen", method = RequestMethod.GET)
	public FseWallet generateWallet(
			@ApiParam(value = "OPTIONAL - DEFAULT-false - passing true will generate a wallet for the test net", required = false) @RequestParam(value = "isTestWallet", required = false) Boolean isTestWallet) {

		String seedVal = DefaultKeyPairService.getInstance().generateSeed();
		Wallet w = DefaultWalletFactory.getInstance().fromSeed(seedVal, isTestWallet == null ? false : isTestWallet);

		return FseWallet.builder().fromClassicAddress(w.classicAddress().value()).isTest(w.isTest())
				.fromPrivateKey(w.privateKey().get()).fromSigningPublicKey(w.publicKey()).userSeed(seedVal).build();

	}

	@ApiOperation(value = "Generates some XRP wallets")
	@RequestMapping(value = "/api/walletgenFromSeed", method = RequestMethod.POST)
	public FseWallet generateWallet(
			@ApiParam(value = "Seed that is used to recover this wallet", required = true) @RequestBody String seed) {



		Wallet w = DefaultWalletFactory.getInstance().fromSeed(seed, false);

		return FseWallet.builder().fromClassicAddress(w.classicAddress().value()).isTest(w.isTest())
				.fromPrivateKey(w.privateKey().get()).fromSigningPublicKey(w.publicKey()).userSeed(seed).build();

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
	@RequestMapping(value = "/api/accountinfo/{classicAddress}", method = RequestMethod.GET)
	public FseAccount accountInfo(
			@ApiParam(value = "Classic XRP address. Example rnL2P...", required = true) @PathVariable("classicAddress") String classicAddress) {
		return xrplService.getAccountInfo(classicAddress);
	}

	@ApiOperation(value = "Distributes tokens to a set of recipient addresses")
	@RequestMapping(value = "/api/payment", method = RequestMethod.POST)
	public FsePaymentResult payment(
			@ApiParam(value = "Payment Details: Click Model under Data Type for details", required = true) @RequestBody FsePaymentRequest paymentRequest) {
		return xrplService.sendFsePayment(paymentRequest);
	}


	@ApiOperation(value = "Distributes tokens to trustline holders.  Airdrop")
	@RequestMapping(value = "/api/payment/trustlines", method = RequestMethod.POST)
	public AirdropSummary paymentTrustlines(
			@ApiParam(value = "Payment Details: Click Model under Data Type for details", required = true) @RequestBody FsePaymentTrustlinesRequest paymentRequest) {
		// DO NOT LOG THE PRIVATE KEY!!
		log.info(
				"payment/trustlines: fromClassicAddress:{} fromSigningPublicKey:{} amount:{} issuerClassicAddress:{}"
					, paymentRequest.getFromClassicAddress(),
				paymentRequest.getFromSigningPublicKey(), paymentRequest.getAmount(),
				paymentRequest.getTrustlineIssuerClassicAddress());
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