package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsTransactionResult;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.transactions.ImmutableIssuedCurrencyAmount;
import org.xrpl.xrpl4j.model.transactions.ImmutablePayment;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedLong;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.entity.BurnTransactionEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.repo.BurnTransactionRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropRecipientRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class TransactionHistoryServiceImpl implements TransactionHistoryService {

	@VisibleForTesting
	@Autowired
	protected XrplClientService xrplClientService;

	@Autowired
	protected PaymentRequestRepo paymentRequestRepo;

	@Autowired
	protected DropRecipientRepo dropRecipientRepo;

	@Autowired
	protected BurnTransactionRepo burnTransactionRepo;

	public static final String TRANSACTION_TYPE_PAYMENT = "PAYMENT";

	private static final int MAX_PAYMENTS_TO_CHECK = 200000;

	public Set<String> getPreviouslyPaidAddresses(String classicAddress, String issuingAddress) {

		List<PaymentRequestEnt> payments = paymentRequestRepo.findAll(Example.of(PaymentRequestEnt.builder()
				.fromClassicAddress(classicAddress).trustlineIssuerClassicAddress(issuingAddress).build()));

		List<Long> paymentRequestIds = payments.stream().map(p -> p.getId()).collect(Collectors.toList());

		List<String> paidAddresses = dropRecipientRepo.findDistinctAddressesByPaymentRequestIds(paymentRequestIds);

		return Sets.newHashSet(paidAddresses);
	}
//	@Cacheable("burnedTokens")
	@Override
	public List<FseTransaction> getTransactionBurns(String classicAddress) {

		List<BurnTransactionEnt> burns = burnTransactionRepo
				.findAll(Example.of(BurnTransactionEnt.builder().toAddress(classicAddress).build()));

		Date oldest = Date.from(LocalDate.parse("2020-10-10").atStartOfDay().toInstant(ZoneOffset.UTC));
		Long minLedgerIndex = Long.MAX_VALUE;
		for (BurnTransactionEnt burn : burns) {
			if (oldest.before(burn.getTxDate())) {
				oldest = burn.getTxDate();// already in the DB so only fetch newer
			}
			if(minLedgerIndex > burn.getLedgerIndex()) {
				minLedgerIndex = burn.getLedgerIndex();
			}
		}

		
		
		List<FseTransaction> transactionsRecent = this.getTransactions(classicAddress, Optional.empty(), 100000,
				Optional.of(oldest));
		
		for(FseTransaction tx : transactionsRecent) {
			if(minLedgerIndex > tx.getLedgerIndex()) {
				minLedgerIndex = tx.getLedgerIndex();
			}
		}
		
		//check for missed transactions
		List<FseTransaction> transactionsOld = this.getTransactions(classicAddress, Optional.of(minLedgerIndex), 50000,
				Optional.of(Date.from(LocalDate.parse("2020-10-10").atStartOfDay().toInstant(ZoneOffset.UTC))));
		
		List<FseTransaction> transactions = new ArrayList<>();
		transactions.addAll(transactionsRecent);
		transactions.addAll(transactionsOld);

		Set<String> hashes = burns.stream().map(b -> b.getTransactionHash()).collect(Collectors.toSet());
		List<BurnTransactionEnt> newTransactions = transactions.stream()
				.filter(t -> "PAYMENT".equals(t.getTransactionType()) 
						&& !hashes.contains(t.getTransactionHash()) 
						&& classicAddress.equals(t.getToAddress()))
				.map(t -> BurnTransactionEnt.builder().amount(t.getAmount().toString()).createDate(new Date())
						.fromAddress(t.getFromAddress())
						.transactionHash(t.getTransactionHash())
						.toAddress(t.getToAddress())
						.ledgerIndex(t.getLedgerIndex())
						.txDate(t.getTransactionDate()).build())
				.collect(Collectors.toList());

		burnTransactionRepo.saveAll(newTransactions);

		List<BurnTransactionEnt> allBurns = new ArrayList<>();
		allBurns.addAll(burns);
		allBurns.addAll(newTransactions);
		return allBurns.stream()
				.map(b -> FseTransaction.builder().transactionDate(b.getTxDate()).fromAddress(b.getFromAddress())
						.ledgerIndex(b.getLedgerIndex())
						.toAddress(b.getToAddress()).transactionHash(b.getTransactionHash()).amount(new BigDecimal(b.getAmount())).build())
				.collect(Collectors.toList());
	}

	/**
	 * calls xrp ledger to validate payments
	 */
	// NEVER CACHE THIS, USED FOR DROP VERIFICATION
	@Override
	public List<FseTransaction> getTransactions(String classicAddress, Optional<Long> maxLedgerIndex, int limit,
			Optional<Date> oldest) {
		try {

			AccountTransactionsResult r = xrplClientService.getTransactions(classicAddress,
					maxLedgerIndex.isPresent() ? Optional.of(LedgerIndex.of(UnsignedLong.valueOf(maxLedgerIndex.get())))
							: Optional.empty());

			List<FseTransaction> allTransactions = new ArrayList<FseTransaction>();
			List<FseTransaction> transactions = r.transactions().stream().map(t -> convert(t))
					.collect(Collectors.toList());

			while (transactions.size() > 0) {
				allTransactions.addAll(transactions.stream().filter(t -> {

					if (oldest.isPresent()) {
						return t.getTransactionDate().after(oldest.get());
					}
					return true;
				}).collect(Collectors.toList()));

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
				if (oldest.isPresent()
						&& transactions.get(transactions.size() - 1).getTransactionDate().before(oldest.get())) {
					break;
				}
				r = xrplClientService.getTransactions(classicAddress,
						Optional.of(LedgerIndex.of(index.get().unsignedLongValue())));

				List<FseTransaction> newTransactions = r.transactions().stream()
						.map(t -> convert(t))
						.collect(Collectors.toList());
				log.info("Found Transaction history after {} for {} size {} will be added to current total found {}",
						oldest, classicAddress, newTransactions.size(), allTransactions.size());
				if (transactions.containsAll(newTransactions)) {
					break;
				}
				transactions = newTransactions;

			}

			return allTransactions;

		} catch (Exception e) {
			log.error("Error fetching transactions " + classicAddress, e);
			throw new RuntimeException("Could not fetch transactions for address " + classicAddress);
		}

	}

	private FseTransaction convert(AccountTransactionsTransactionResult<?> t) {

		FseTransaction tx = FseTransaction.builder()
				.transactionHash(t.transaction().hash().isPresent() ? t.transaction().hash().get().value() : null)
				.resultCode(t.metadata().isEmpty() ? null : t.metadata().get().transactionResult())
				.transactionDate(convertDateFromUnsignedLong(
						t.transaction().closeDate().isEmpty() ? UnsignedLong.ONE : t.transaction().closeDate().get()))
				.transactionType(t.transaction().transactionType().toString()).issuerAddress(convertIssuer(t))
				.amount(convertPayment(t)).issuerAddress(convertIssuer(t)).toAddress(convertDestination(t))
				.fromAddress(t.transaction().account().toString()).ledgerIndex(convertLedgerIndex(t))
				.currency(convertCurrency(t)).build();
		
		return tx;
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

	// NEVER CACHE THIS, USED FOR DROP VERIFICATION
	@Override
	public List<FseTransaction> getTransactionsBetweenDates(String classicAddress, Date startTime, Date stopTime) {

		List<FseTransaction> transactionsInWindow = new ArrayList<FseTransaction>();
		List<FseTransaction> transactions = this.getTransactions(classicAddress, Optional.empty(),
				MAX_PAYMENTS_TO_CHECK, Optional.of(startTime));

		transactionsInWindow.addAll(transactions.stream()
				.filter(t -> t.getTransactionDate().after(startTime) && t.getTransactionDate().before(stopTime))
				.collect(Collectors.toList()));

		return transactionsInWindow;

	}

}
