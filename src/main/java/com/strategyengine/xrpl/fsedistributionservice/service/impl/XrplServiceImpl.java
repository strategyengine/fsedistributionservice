package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;
import org.xrpl.xrpl4j.model.client.accounts.TrustLine;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.service.CurrencyHexService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;
import com.strategyengine.xrpl.fsedistributionservice.service.ValidationService;
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

	@VisibleForTesting
	@Autowired
	protected ValidationService validationService;
	
	@VisibleForTesting
	@Autowired
	protected TransactionHistoryService transactionHistoryService;

	@Cacheable("trustline-cache")
	@Override
	public List<FseTrustLine> getTrustLines(String classicAddress, Optional<String> currency, boolean includes) {
		try {

			validationService.validateClassicAddress(classicAddress);
			AccountLinesResult trustLines = xrplClientService.getTrustLines(classicAddress);

			return trustLines.lines().stream().filter(t -> acceptByCurrency(t, currency, includes))
					.map(t -> FseTrustLine.builder().classicAddress(t.account().value()).currency(t.currency())
							.balance(t.balance()).build())
					.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("Error getting trustlines", e);
		}
		return null;
	}

	@Override
	public List<FseTrustLine> getTrustLines(String classicAddress) {
		validationService.validateClassicAddress(classicAddress);
		return getTrustLines(classicAddress, Optional.empty(), true);
	}

	private boolean acceptByCurrency(TrustLine t, Optional<String> currency, boolean includes) {
		if (currency.isEmpty()) {
			return true;
		}
		return includes ? currency.get().equals(t.currency()) : !currency.get().equals(t.currency());
	}

	@Override
	public FseAccount getAccountInfo(String classicAddress) {
		validationService.validateClassicAddress(classicAddress);
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
		validationService.validate(paymentRequest);
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
		
		
		final Set<String> paidAddresses;
		
		if(p.isZeroBalanceOnly()) {
			paidAddresses = transactionHistoryService.getPreviouslyPaidAddresses(p.getFromClassicAddress(), p.getCurrencyName(), p.getTrustlineIssuerClassicAddress());
		}else {
			paidAddresses = new HashSet<String>();
		}

		validationService.validate(p);
		List<FseTrustLine> trustLines = getTrustLines(p.getTrustlineIssuerClassicAddress());

		FseAccount fromAccount = getAccountInfo(p.getFromClassicAddress());

		Optional<FseTrustLine> fromAddressTrustLine = trustLines.stream()
				.filter(t -> p.getFromClassicAddress().equals(t.getClassicAddress())
						&& p.getCurrencyName().equals(t.getCurrency()))
				.findFirst();
		
		//filter out trustlines that are not elibigle for the final payment list
		List<FseTrustLine> eligibleTrustLines = trustLines.stream()
				.filter(t -> p.isZeroBalanceOnly() ? Double.valueOf(t.getBalance()) == 0 : true)
				.filter(t -> p.isZeroBalanceOnly() ? !paidAddresses.contains(t.getClassicAddress()) : true)//don't pay this address if it received a payment but just moved the tokens to be at 0 again
				.collect(Collectors.toList());
		
		
		validationService.validateXrpBalance(fromAccount.getBalance(), eligibleTrustLines.size());
		validationService.validateDistributingTokenBalance(fromAddressTrustLine, p.getAmount(), eligibleTrustLines.size());

		log.info("Found eligible TrustLines to send to.  Size: {}", trustLines.size());
		List<FsePaymentResult> results = eligibleTrustLines.stream()
				.map(t -> sendFsePayment(
						FsePaymentRequest.builder().trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress())
								.currencyName(p.getCurrencyName()).amount(p.getAmount())
								.fromClassicAddress(p.getFromClassicAddress()).fromPrivateKey(p.getFromPrivateKey())
								.fromSigningPublicKey(p.getFromSigningPublicKey())
								.toClassicAddresses(asList(t.getClassicAddress())).build()))
				.collect(Collectors.toList());

		return results;

	}



	private List<String> asList(String i) {
		return ImmutableList.of(i);
	}
}
