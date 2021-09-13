package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.xrpl.xrpl4j.keypairs.DefaultKeyPairService;
import org.xrpl.xrpl4j.wallet.DefaultWalletFactory;
import org.xrpl.xrpl4j.wallet.Wallet;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesMinTriggeredRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.model.FseWallet;
import com.strategyengine.xrpl.fsedistributionservice.service.TrustlineTriggerDropService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = "XRPL Trustline endpoints")
@RestController
public class XrplController {

	@VisibleForTesting
	@Autowired
	protected XrplService xrplService;

	@VisibleForTesting
	@Autowired
	protected TrustlineTriggerDropService trustlineTriggerDropService;

	@ApiOperation(value = "Get the Trustlines for an XRP address")
	@RequestMapping(value = "/api/trustlines/{classicAddress}", method = RequestMethod.GET)
	public List<FseTrustLine> trustLines(
			@ApiParam(value = "Classic XRP address. Example rnL2P...", required = true) @PathVariable("classicAddress") String classicAddress,
			@ApiParam(value = "OPTOINAL - Enter a currency to include or exclude from the response. DEFAULT is include", required = false) @RequestParam(value="filterCurrency", required=false) String filterCurrency,
			@ApiParam(value = "OPTIONAL - true will only return results with the currency parameter, false will return all results not having the currency param", required = false) @RequestParam(value="includeFilter", required=false) Boolean includeFilter) {
		
		return xrplService.getTrustLines(classicAddress, Optional.ofNullable(filterCurrency), includeFilter==null? true: includeFilter);
	}

	
	@ApiOperation(value = "Generates some XRP wallets")
	@RequestMapping(value = "/api/walletgen", method = RequestMethod.GET)
	public FseWallet generateWallet(
			@ApiParam(value = "OPTIONAL - DEFAULT-false - passing true will generate a wallet for the test net", required = false) @RequestParam(value="isTestWallet", required=false) Boolean isTestWallet) {
		
		String seed = DefaultKeyPairService.getInstance().generateSeed();
		Wallet w  =  DefaultWalletFactory.getInstance().fromSeed(seed, isTestWallet==null? false: isTestWallet);

		return FseWallet.builder().fromClassicAddress(w.classicAddress().value())
				.isTest(w.isTest())
				.fromPrivateKey(w.privateKey().get())
				.fromSigningPublicKey(w.publicKey())
				.userSeed(seed).build();
		
		
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

	@ApiOperation(value = "Distributes tokens to trustline holders")
	@RequestMapping(value = "/api/payment/trustlines", method = RequestMethod.POST)
	public List<FsePaymentResult> paymentTrustlines(
			@ApiParam(value = "Payment Details: Click Model under Data Type for details", required = true) @RequestBody FsePaymentTrustlinesRequest paymentRequest) {
		return xrplService.sendFsePaymentToTrustlines(paymentRequest);
	}

	@ApiOperation(value = "Distributes tokens to trustline holders only after a minimum number of trustlines have been created")
	@RequestMapping(value = "/api/payment/trustlines/min/airdrop", method = RequestMethod.POST)
	public void paymentTrustlinesMinAirdrop(
			@ApiParam(value = "Payment Details: Click Model under Data Type for details", required = true) @RequestBody FsePaymentTrustlinesMinTriggeredRequest paymentRequest) {
		// this will block a http accept thread. Add a thread pool to call this if you
		// are going to call a lot of these.
		trustlineTriggerDropService.triggerAirdropAfterMinTrustlines(paymentRequest);
	}
	
	
	private void validate(FsePaymentRequest payment) {
		
		if(payment.getDestinationTag()!=null) {
			try {
				Integer.parseInt(payment.getDestinationTag());
			}catch(Exception e) {
				throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Destination Tag is invalid.  Needs to be removed or set to a number");
			}
		}
	}

}