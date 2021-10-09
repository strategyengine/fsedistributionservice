package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;
import org.xrpl.xrpl4j.model.client.accounts.TrustLine;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.model.AirdropSummary;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResults;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropSummaryService;
import com.strategyengine.xrpl.fsedistributionservice.service.CurrencyHexService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;
import com.strategyengine.xrpl.fsedistributionservice.service.ValidationService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import lombok.NonNull;
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
	protected AirdropSummaryService airdropSummaryService;

	@VisibleForTesting
	@Autowired
	protected TransactionHistoryService transactionHistoryService;

	public static final String SERVICE_FEE = "1";

	// you're a dev, do as thou wilt
	private static final String SERVICE_FEE_ADDRESS = "rNP3mFp8QGe1yXYMdypU7B5rXM1HK9VbDK";

	@Cacheable("trustline-cache")
	@Override
	public List<FseTrustLine> getTrustLines(String classicAddress, Optional<String> currency, boolean includes, boolean sortByRich) {
		try {

			validationService.validateClassicAddress(classicAddress);
			AccountLinesResult trustLines = xrplClientService.getTrustLines(classicAddress);

			List<FseTrustLine> fseTrustLines = trustLines.lines().stream()
					.filter(t -> acceptByCurrency(t, currency, includes))
					.map(t -> FseTrustLine.builder().classicAddress(t.account().value()).currency(t.currency())
							.balance(t.balance().replaceFirst("-", "")).build())
					.collect(Collectors.toList());

			if(sortByRich) {
				fseTrustLines.sort((a, b) -> (Double.valueOf(a.getBalance()).compareTo(Double.valueOf(b.getBalance()))));
			}else {
				//put the oldest trustlines first, OG sort
				Collections.reverse(fseTrustLines);
			}
			return fseTrustLines;

		} catch (Exception e) {
			log.error("Error getting trustlines", e);
		}
		return null;
	}

	@Override
	public List<FseTrustLine> getTrustLines(String classicAddress, boolean sortByRich) {
		validationService.validateClassicAddress(classicAddress);
		return getTrustLines(classicAddress, Optional.empty(), true, sortByRich);
	}

	private boolean acceptByCurrency(TrustLine t, Optional<String> currency, boolean includes) {
		if (currency.isEmpty()) {
			return true;
		}
		return includes ? currency.get().equals(t.currency()) : !currency.get().equals(t.currency());
	}

	@Override
	public List<FseAccount> getAccountInfo(List<String> classicAddresses) {
		return classicAddresses.stream().map(a -> getAccountInfo(a)).collect(Collectors.toList());
	}

		
	private FseAccount getAccountInfo(String classicAddress) {
		validationService.validateClassicAddress(classicAddress);
		try {
			FseAccount account =  xrplClientService.getAccountInfo(classicAddress);

			return account.toBuilder().trustLines(getTrustLines(classicAddress, true)).build();
		} catch (Exception e) {
			log.error("Error getting account info", e);
		}
		return null;
	}

	@Override
	public List<FsePaymentResult> sendFsePayment(FsePaymentRequest paymentRequest) {
		if (!paymentRequest.isAgreeFee()) {
			throw new BadRequestException(
					"This transactions requires you to agree to the service fee.  Help keep this service running in the cloud");
		}
		validationService.validate(paymentRequest);
		allowPayment(paymentRequest.toBuilder().payBlacklistedAddresses(paymentRequest.isPayBlacklistedAddresses()).amount(SERVICE_FEE).currencyName("XRP")
				.toClassicAddresses(ImmutableList.of(SERVICE_FEE_ADDRESS)).build(), new AtomicInteger(0));

		List<FsePaymentResult> results = allowPayment(paymentRequest, new AtomicInteger(0));
		return results;
	}

	private List<FsePaymentResult> allowPayment(FsePaymentRequest paymentRequest, AtomicInteger count) {

		validationService.validate(paymentRequest);
		try {
			List<FsePaymentResult> results = xrplClientService.sendFSEPayment(paymentRequest);

			log.info("Payment to: {} for currency: {} counter:{}", paymentRequest.getToClassicAddresses(),
					paymentRequest.getCurrencyName(), String.valueOf(count.getAndIncrement()));

			return results;
		} catch (Exception e) {
			log.error("Error sending payment to " + paymentRequest, e);
		}

		return null;

	}

	@Override
	public FsePaymentResults sendFsePaymentToTrustlines(@NonNull FsePaymentTrustlinesRequest p) {

		Date startTime = new Date();

		if (!p.isAgreeFee()) {
			throw new BadRequestException(
					"This transactions requires you to agree to the service fee.  Help keep this service running in the cloud");
		}

		final Set<String> previouslyPaidAddresses;

		if (p.isNewTrustlinesOnly()) {
			previouslyPaidAddresses = transactionHistoryService.getPreviouslyPaidAddresses(p.getFromClassicAddress(),
					p.getCurrencyName(), p.getTrustlineIssuerClassicAddress());
		} else {
			previouslyPaidAddresses = new HashSet<String>();
		}

		validationService.validate(p);

		List<FseTrustLine> trustLines = getTrustLines(p.getTrustlineIssuerClassicAddress(), false);

		// filter out trustlines that are not elibigle for the final payment list
		List<FseTrustLine> eligibleTrustLines = trustLines.stream()
				.filter(t -> p.isNewTrustlinesOnly() ? !previouslyPaidAddresses.contains(t.getClassicAddress()) : true)// don't
																														// pay
				// this
				// address
				// if it
				// received
				// a payment
				// but just
				// moved the
				// tokens to
				// be at 0
				// again
				.collect(Collectors.toList());

		
		Optional<FseTrustLine> fromAddressTrustLine = trustLines.stream()
				.filter(t -> p.getFromClassicAddress().equals(t.getClassicAddress())
						&& p.getCurrencyName().equals(t.getCurrency()))
				.findFirst();
		
		if(p.getMaximumTrustlines()!=null &&  p.getMaximumTrustlines() < trustLines.size()) {
			
			eligibleTrustLines = eligibleTrustLines.subList(0, p.getMaximumTrustlines());
		}

		FseAccount fromAccount = getAccountInfo(p.getFromClassicAddress());


		validationService.validateXrpBalance(fromAccount.getXrpBalance(), eligibleTrustLines.size());
		validationService.validateDistributingTokenBalance(fromAddressTrustLine, p.getAmount(),
				eligibleTrustLines.size());

		if (eligibleTrustLines.isEmpty()) {
			return FsePaymentResults.builder().build();
		}

		// pay service fee
		allowPayment(
				FsePaymentRequest.builder().payBlacklistedAddresses(true).trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress())
						.currencyName("XRP").amount(SERVICE_FEE).fromClassicAddress(p.getFromClassicAddress())
						.fromPrivateKey(p.getFromPrivateKey()).fromSigningPublicKey(p.getFromSigningPublicKey())
						.toClassicAddresses(asList(SERVICE_FEE_ADDRESS)).build(),
				new AtomicInteger(0));

		log.info("Found eligible TrustLines to send to.  Size: {}", eligibleTrustLines.size());

		AtomicInteger count = new AtomicInteger(0);

		List<FsePaymentResult> results = eligibleTrustLines.stream().map(t -> allowPayment(FsePaymentRequest.builder()
				.trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress()).currencyName(p.getCurrencyName())
				.amount(p.getAmount()).agreeFee(p.isAgreeFee()).fromClassicAddress(p.getFromClassicAddress())
				.fromPrivateKey(p.getFromPrivateKey()).fromSigningPublicKey(p.getFromSigningPublicKey())
				.toClassicAddresses(asList(t.getClassicAddress())).build(), count)).flatMap(List::stream).collect(Collectors.toList());
		
		return FsePaymentResults.builder().results(results).start(startTime).end(new Date()).transactionCount(count.get()).build();

	}

	private List<String> asList(String i) {
		return ImmutableList.of(i);
	}

}
