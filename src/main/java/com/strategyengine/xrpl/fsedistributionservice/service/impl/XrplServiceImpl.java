package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.service.CurrencyHexService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class XrplServiceImpl implements XrplService {

	@VisibleForTesting
	@Autowired
	protected XrplClientService xrplClientService;

	@VisibleForTesting
	@Autowired
	protected CurrencyHexService currencyHexService;

	@Override
	public List<FseTrustLine> getTrustLines(String classicAddress) {
		try {
			AccountLinesResult trustLines = xrplClientService.getTrustLines(classicAddress);

			return trustLines.lines().stream().map(t -> FseTrustLine.builder().classicAddress(t.account().value())
					.currency(t.currency()).balance(t.balance()).build()).collect(Collectors.toList());

		} catch (Exception e) {
			log.error("Error getting trustlines", e);
		}
		return null;
	}

	@Override
	public FseAccount getAccountInfo(String classicAddress) {
		try {
			AccountInfoResult account = xrplClientService.getAccountInfo(classicAddress);

			return FseAccount.builder().classicAddress(account.accountData().account().value())
					.balance(account.accountData().balance().toXrp()).build();
		} catch (Exception e) {
			log.error("Error getting account info", e);
		}
		return null;
	}

	@Override
	public FsePaymentResult sendFsePayment(FsePaymentRequest paymentRequest) {
		try {
			List<String> responseMessage = xrplClientService.sendFSEPayment(paymentRequest);

			return FsePaymentResult.builder().responseMessages(responseMessage).build();
		} catch (Exception e) {
			log.error("Error sending payment to " + paymentRequest, e);
		}

		return null;

	}

	@Override
	public List<FsePaymentResult> sendFsePaymentToTrustlines(FsePaymentTrustlinesRequest p) {

		List<FseTrustLine> trustLines = getTrustLines(p.getTrustlineIssuerClassicAddress());

		List<FsePaymentResult> results = trustLines.stream()
				.filter(t -> p.isZeroBalanceOnly() ? Double.valueOf(t.getBalance()) == 0 : true).filter(t -> accept(t))
				.filter(t -> currencyHexService.isAcceptedCurrency(t, p.getCurrencyName())) // validate hex currency or
																							// 3 character currency
				.map(t -> sendFsePayment(
						FsePaymentRequest.builder().trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress())
								.currencyName(t.getCurrency()).amount(p.getAmount())
								.fromClassicAddress(p.getFromClassicAddress()).fromPrivateKey(p.getFromPrivateKey())
								.fromSigningPublicKey(p.getFromSigningPublicKey())
								.toClassicAddresses(asList(t.getClassicAddress())).build()))
				.collect(Collectors.toList());

		return results;

	}

	// add any filtration heres
	private boolean accept(FseTrustLine t) {

		return true;
	}

	private List<String> asList(String i) {
		return ImmutableList.of(i);
	}
}
