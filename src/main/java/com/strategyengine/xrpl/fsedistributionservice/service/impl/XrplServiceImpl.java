package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsTransactionResult;
import org.xrpl.xrpl4j.model.client.accounts.TrustLine;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.transactions.ImmutableIssuedCurrencyAmount;
import org.xrpl.xrpl4j.model.transactions.ImmutablePayment;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedLong;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
import com.strategyengine.xrpl.fsedistributionservice.service.CurrencyHexService;
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

		validationService.validate(p);
		List<FseTrustLine> trustLines = getTrustLines(p.getTrustlineIssuerClassicAddress());

		FseAccount fromAccount = getAccountInfo(p.getFromClassicAddress());
		validationService.validateXrpBalance(fromAccount.getBalance(), trustLines.size());

		Optional<FseTrustLine> fromAddressTrustLine = trustLines.stream()
				.filter(t -> p.getFromClassicAddress().equals(t.getClassicAddress())
						&& p.getCurrencyName().equals(t.getCurrency()))
				.findFirst();
		validationService.validateDistributingTokenBalance(fromAddressTrustLine, p.getAmount(), trustLines.size());

		log.info("Found trustlines to send to.  Size: {}", trustLines.size());
		List<FsePaymentResult> results = trustLines.stream()
				.filter(t -> p.isZeroBalanceOnly() ? Double.valueOf(t.getBalance()) == 0 : true).filter(t -> accept(t))
				.map(t -> sendFsePayment(
						FsePaymentRequest.builder().trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress())
								.currencyName(p.getCurrencyName()).amount(p.getAmount())
								.fromClassicAddress(p.getFromClassicAddress()).fromPrivateKey(p.getFromPrivateKey())
								.fromSigningPublicKey(p.getFromSigningPublicKey())
								.toClassicAddresses(asList(t.getClassicAddress())).build()))
				.collect(Collectors.toList());

		return results;

	}

	@Override
	public List<FseTransaction> getTransactions(String classicAddress, Optional<Long> maxLedgerIndex, int limit) {
		try {
			
			AccountTransactionsResult r = xrplClientService.getTransactions(classicAddress, Optional.empty());
			
			List<FseTransaction> allTransactions = new ArrayList<FseTransaction>();
			List<FseTransaction> transactions = r.transactions().stream().map(t -> convert(t)).collect(Collectors.toList());
			
			while(transactions.size()>0) {
				allTransactions.addAll(transactions);
				
				//TODO fix this -1, since there could be multiple transactions in a single ledger index.  
				// need to fetch the same ledger index and dedup
				
				Optional<LedgerIndex> index = r.transactions().get(r.transactions().size()-1).transaction().ledgerIndex();
				if(index.isEmpty()) {
					break;
				}
				
				r = xrplClientService.getTransactions(classicAddress, Optional.of(LedgerIndex.of(index.get().unsignedLongValue().minus(UnsignedLong.ONE))));
				transactions = r.transactions().stream().map(t -> convert(t)).collect(Collectors.toList());
				
				if(allTransactions.size()> 1000) {
					allTransactions = allTransactions.subList(0, limit);
					break;
				}
			}
			
			return allTransactions;
			
		} catch (Exception e) {
			log.error("Error fetching transactions " + classicAddress, e);
			throw new BadRequestException(
					"Could not fetch transactions for address " + classicAddress);
		}

	}

	// add any filtration heres
	private boolean accept(FseTrustLine t) {

		return true;
	}

	private List<String> asList(String i) {
		return ImmutableList.of(i);
	}

	private FseTransaction convert(AccountTransactionsTransactionResult<?> t) {
		return FseTransaction.builder().transactionDate(convertDateFromUnsignedLong(t.transaction().closeDate().get()))
				.transactionType(t.transaction().transactionType().toString()).issuerAddress(convertIssuer(t))
				.amount(convertPayment(t))
				.issuerAddress(convertIssuer(t))
				.toAddress(convertDestination(t))
				.fromAddress(t.transaction().account().toString())
				.ledgerIndex(convertLedgerIndex(t))
				.currency(convertCurrency(t)).build();
	}

	private Long convertLedgerIndex(AccountTransactionsTransactionResult<?> t) {
		if(t.transaction().ledgerIndex().isEmpty()) {
			return null;
		}
		return Long.parseLong(t.transaction().ledgerIndex().get().value());
	}

	private String convertDestination(AccountTransactionsTransactionResult<?> t) {

		if (t.transaction() instanceof ImmutablePayment) {
			ImmutablePayment payment = (ImmutablePayment) t.transaction();

			return payment.destination().toString();
		}
		return null;
	}
	
	
	private String convertCurrency(AccountTransactionsTransactionResult<?> t) {

		if (t.transaction() instanceof ImmutablePayment) {
			ImmutablePayment payment = (ImmutablePayment) t.transaction();

			if (payment.amount() instanceof ImmutableIssuedCurrencyAmount) {
				ImmutableIssuedCurrencyAmount amount = (ImmutableIssuedCurrencyAmount) payment.amount();
				return amount.currency();
			}
		}
		return "XRP";
	}

	private BigDecimal convertPayment(AccountTransactionsTransactionResult<?> t) {

		if (t.transaction() instanceof ImmutablePayment) {
			ImmutablePayment payment = (ImmutablePayment) t.transaction();

			if (payment.amount() instanceof ImmutableIssuedCurrencyAmount) {
				ImmutableIssuedCurrencyAmount amount = (ImmutableIssuedCurrencyAmount) payment.amount();
				return new BigDecimal(amount.value());
			}
			if (payment.amount() instanceof XrpCurrencyAmount) {
				XrpCurrencyAmount amount = (XrpCurrencyAmount) payment.amount();
				return amount.toXrp();
			}

		}
		return null;

	}

	private String convertIssuer(AccountTransactionsTransactionResult<?> t) {
		if (t.transaction() instanceof ImmutablePayment) {
			ImmutablePayment payment = (ImmutablePayment) t.transaction();

			if (payment.amount() instanceof ImmutableIssuedCurrencyAmount) {
				ImmutableIssuedCurrencyAmount amount = (ImmutableIssuedCurrencyAmount) payment.amount();
				return amount.issuer().toString();
			}
		}
		return null;
	}

	private Date convertDateFromUnsignedLong(UnsignedLong t) {
		long rippleEpoch = 946684800000l;
		long millisSinceEpoch = t.longValue()*1000;

		return new Date(rippleEpoch + millisSinceEpoch);

	}
}
