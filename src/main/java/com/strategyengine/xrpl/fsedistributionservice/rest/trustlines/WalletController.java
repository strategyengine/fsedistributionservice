package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.xrpl.xrpl4j.keypairs.DefaultKeyPairService;
import org.xrpl.xrpl4j.wallet.DefaultWalletFactory;
import org.xrpl.xrpl4j.wallet.Wallet;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.model.FseWallet;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropSummaryService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;
import com.strategyengine.xrpl.fsedistributionservice.service.TrustlineTriggerDropService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Api(tags = "Wallet generation endpoints")
@RestController
public class WalletController {

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

	

	@ApiOperation(value = "Generates some XRP wallets")
	@RequestMapping(value = "/api/walletgen", method = RequestMethod.GET)
	public FseWallet generateWallet() {

		String seedVal = DefaultKeyPairService.getInstance().generateSeed();
		Wallet w = DefaultWalletFactory.getInstance().fromSeed(seedVal, false);

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
	
	

}