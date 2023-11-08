package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.wallet.DefaultWalletFactory;
import org.xrpl.xrpl4j.wallet.Wallet;
import org.xrpl.xrpl4j.wallet.WalletFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.PaymentType;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.XrplNetwork;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.service.ConfigService;
import com.strategyengine.xrpl.fsedistributionservice.service.PaymentService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class AutoBurnRunnerImpl {

	@VisibleForTesting
	@Autowired
	protected PaymentService paymentService;

	@VisibleForTesting
	@Autowired
	protected ConfigService configService;

	@Autowired
	protected XrplService xrplService;

	@Autowired
	protected XrplClientService xrplClientService;
	
	private String environment = System.getenv("ENV");

	// automatically burns FSE and SSE held by the configured account
	@Scheduled(cron = "0 1 1 * * ?")
	public void autoBurn() {

		if (!"BEAST1".equals(environment)){
			return;
		}
		
		XrplNetwork xrplNetwork = XrplNetwork.XRPL_MAIN;
		
		String seed = configService.getAutoBurnSeed();

		WalletFactory walletFactory = DefaultWalletFactory.getInstance();

		final Wallet wallet = walletFactory.fromSeed(seed, false);

		FseAccount account = xrplService.getAccountInfo(ImmutableList.of(wallet.classicAddress().value()), true).get(0);

		burnOne("rs1MKY54miDtMFEGyNuPd3BLsXauFZUSrj", wallet, account.getTrustLines(), xrplNetwork);
		burnOne("rMDQTunsjE32sAkBDbwixpWr8TJdN5YLxu", wallet, account.getTrustLines(), xrplNetwork);

	}

	private void burnOne(String issuer, Wallet wallet, List<FseTrustLine> trustLines, XrplNetwork xrplNetwork) {

		Optional<FseTrustLine> trustLine = trustLines.stream().filter(t -> t.getClassicAddress().equals(issuer))
				.findFirst();

		if (trustLine.isEmpty()) {
			return;
		}
		try {
			
			if(new BigDecimal(trustLine.get().getBalance()).compareTo(new BigDecimal("1"))<=0) {
				return;
			}
			
			xrplClientService.sendFSEPayment(
					FsePaymentRequest.builder().trustlineIssuerClassicAddress(trustLine.get().getClassicAddress())
							.agreeFee(true).amount("1")
							.currencyName(trustLine.get().getCurrency())
							.fromClassicAddress(wallet.classicAddress().value())
							.fromPrivateKey(wallet.privateKey().get()).fromSigningPublicKey(wallet.publicKey())
							.paymentType(PaymentType.FLAT).toClassicAddresses(ImmutableList.of(issuer)).build(),
					DropRecipientEnt.builder().address(issuer).createDate(new Date())
							.payAmount("1").build(), xrplNetwork);
		} catch (Exception e) {
			log.error("Autoburn failure ", e);
		}

	}
}
