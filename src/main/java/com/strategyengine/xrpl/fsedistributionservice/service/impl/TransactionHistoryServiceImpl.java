package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsTransactionResult;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.transactions.ImmutableIssuedCurrencyAmount;
import org.xrpl.xrpl4j.model.transactions.ImmutablePayment;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedLong;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class TransactionHistoryServiceImpl implements TransactionHistoryService {

	@VisibleForTesting
	@Autowired
	protected XrplClientService xrplClientService;

	private static final String TRANSACTION_TYPE_PAYMENT = "PAYMENT";

	private static final int MAX_PAYMENTS_TO_CHECK = 50000;

	@Override
	public Set<String> getPreviouslyPaidAddresses(String classicAddress, String currency, String issuingAddress) {

		List<FseTransaction> payments = getTransactions(classicAddress, Optional.empty(), MAX_PAYMENTS_TO_CHECK);
		
		Set<String> paidAddresses = payments.stream().filter(t -> t.getTransactionType().equals(TRANSACTION_TYPE_PAYMENT) && 
				classicAddress.equals(t.getFromAddress()) && currency.equals(t.getCurrency()) && issuingAddress.equals(t.getIssuerAddress()))
		.map(t -> t.getToAddress()).collect(Collectors.toSet());
		
		return paidAddresses;
	}

	@Override
	public List<FseTransaction> getTransactions(String classicAddress, Optional<Long> maxLedgerIndex, int limit) {
		try {

			AccountTransactionsResult r = xrplClientService.getTransactions(classicAddress, Optional.empty());

			List<FseTransaction> allTransactions = new ArrayList<FseTransaction>();
			List<FseTransaction> transactions = r.transactions().stream().map(t -> convert(t))
					.collect(Collectors.toList());

			while (transactions.size() > 0) {
				allTransactions.addAll(transactions);

				if (allTransactions.size() > limit) {
					allTransactions = allTransactions.subList(0, limit);
					break;
				}

				// min ledger index returned
				Optional<LedgerIndex> index = r.transactions().get(r.transactions().size() - 1).transaction()
						.ledgerIndex();
				if (index.isEmpty()) {
					break;
				}

				r = xrplClientService.getTransactions(classicAddress,
						Optional.of(LedgerIndex.of(index.get().unsignedLongValue())));

				List<FseTransaction> newTransactions = r.transactions().stream().map(t -> convert(t))
						.collect(Collectors.toList());
				if (transactions.containsAll(newTransactions)) {
					break;
				}
				transactions = newTransactions;

			}

			return allTransactions;

		} catch (Exception e) {
			log.error("Error fetching transactions " + classicAddress, e);
			throw new BadRequestException("Could not fetch transactions for address " + classicAddress);
		}

	}

	private FseTransaction convert(AccountTransactionsTransactionResult<?> t) {
		return FseTransaction.builder().transactionDate(convertDateFromUnsignedLong(t.transaction().closeDate().get()))
				.transactionType(t.transaction().transactionType().toString()).issuerAddress(convertIssuer(t))
				.amount(convertPayment(t)).issuerAddress(convertIssuer(t)).toAddress(convertDestination(t))
				.fromAddress(t.transaction().account().toString()).ledgerIndex(convertLedgerIndex(t))
				.currency(convertCurrency(t)).build();
	}

	private Long convertLedgerIndex(AccountTransactionsTransactionResult<?> t) {
		if (t.transaction().ledgerIndex().isEmpty()) {
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
		long millisSinceEpoch = t.longValue() * 1000 + rippleEpoch;
		
		return new Date(millisSinceEpoch);

	}

}
