package com.strategyengine.xrpl.fsedistributionservice.client.xrp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.codec.addresses.exceptions.EncodingFormatException;
import org.xrpl.xrpl4j.crypto.KeyMetadata;
import org.xrpl.xrpl4j.crypto.PrivateKey;
import org.xrpl.xrpl4j.crypto.signing.SignatureService;
import org.xrpl.xrpl4j.crypto.signing.SignedTransaction;
import org.xrpl.xrpl4j.crypto.signing.SingleKeySignatureService;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsResult;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.client.fees.FeeResult;
import org.xrpl.xrpl4j.model.client.ledger.LedgerRequestParams;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.client.transactions.TransactionRequestParams;
import org.xrpl.xrpl4j.model.client.transactions.TransactionResult;
import org.xrpl.xrpl4j.model.immutables.FluentCompareTo;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.CurrencyAmount;
import org.xrpl.xrpl4j.model.transactions.IssuedCurrencyAmount;
import org.xrpl.xrpl4j.model.transactions.Marker;
import org.xrpl.xrpl4j.model.transactions.Payment;
import org.xrpl.xrpl4j.model.transactions.Transaction;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class XrplClientServiceImpl implements XrplClientService {

	@Autowired
	private XrplClient xrplClient;

	private ExecutorService executor = Executors.newFixedThreadPool(100);

	private Set<String> blackListedAddresses = null;

	@PreDestroy
	public void shutdownExecutor() {
		executor.shutdown();
	}

	@PostConstruct
	public void init() throws Exception {

		try (InputStream inputStream = getClass().getResourceAsStream("/blacklisted.txt");
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			blackListedAddresses = reader.lines().collect(Collectors.toSet());
		}

	}

	@Override
	public AccountInfoResult getAccountInfo(String classicAddress) throws Exception {

		final Address address = Address.builder().value(classicAddress).build();

		final AccountInfoRequestParams requestParams = AccountInfoRequestParams.of(address);
		final AccountInfoResult accountInfoResult = xrplClient.accountInfo(requestParams);

		return accountInfoResult;
	}

	@Override
	public AccountTransactionsResult getTransactions(String classicAddress, Optional<LedgerIndex> maxLedger)
			throws Exception {

		AccountTransactionsResult txs = xrplClient.accountTransactions(AccountTransactionsRequestParams.builder()
				.account(Address.of(classicAddress)).ledgerIndexMax(maxLedger).build());

		return txs;

	}

	@Override
	public AccountLinesResult getTrustLines(String classicAddress) throws Exception {
		final Address address = Address.builder().value(classicAddress).build();

		AccountLinesResult result = null;
		AccountLinesResult pageResult = null;
		Marker marker = null;
		while (pageResult == null || !pageResult.lines().isEmpty()) {

			if (pageResult != null && marker == null) {
				break;
			}

			AccountLinesRequestParams requestParams = null;
			if (marker != null) {
				requestParams = AccountLinesRequestParams.builder().marker(marker).account(address)
						.limit(UnsignedInteger.valueOf(100000)).build();
			} else {
				requestParams = AccountLinesRequestParams.builder().account(address)
						.limit(UnsignedInteger.valueOf(100000)).build();
			}

			pageResult = xrplClient.accountLines(requestParams);

			if (result == null) {
				result = pageResult;
			} else {
				result = AccountLinesResult.builder().account(result.account()).addAllLines(pageResult.lines())
						.addAllLines(result.lines()).ledgerCurrentIndex(pageResult.ledgerCurrentIndex())
						.marker(pageResult.marker()).build();
			}

			marker = pageResult.marker().isPresent() ? pageResult.marker().get() : null;
		}

		return result;

	}

	@Override
	public List<String> sendFSEPayment(FsePaymentRequest paymentRequest) {

		return paymentRequest.getToClassicAddresses().stream().map(a -> sendFSEPayment(paymentRequest, a, 0))
				.collect(Collectors.toList());
	}

	private String sendFSEPayment(FsePaymentRequest paymentRequest, String toClassicAddress, int attempt) {

		if(attempt > 10) {
			log.error("Completely failed to sendFSEPayment " + paymentRequest + " " + toClassicAddress);
			return "Failed max attempts " + toClassicAddress ;
		}
		if (blackListedAddresses.contains(toClassicAddress)) {
			log.info("Skipping blacklisted address " + toClassicAddress);
			return "Blacklisted: " + toClassicAddress;
		}
		attempt++;
		try {

			String fromClassicAddress = paymentRequest.getFromClassicAddress();

			if (fromClassicAddress.equals(toClassicAddress)) {
				return "Not sending to self";
			}

			String amount = paymentRequest.getAmount();
			String privateKeyStr = paymentRequest.getFromPrivateKey();
			String destinationTag = paymentRequest.getDestinationTag();

			// Request current fee information from rippled
			final FeeResult feeResult = waitForReasonableFee(xrplClient);
			final XrpCurrencyAmount openLedgerFee = feeResult.drops().openLedgerFee();

			if (openLedgerFee.toXrp().compareTo(new BigDecimal(".0002")) > 0) {
				log.warn("Fee is too high! " + openLedgerFee.toXrp());
			}

			// Construct a Payment
			// Workaround for https://github.com/XRPLF/xrpl4j/issues/84
			final LedgerIndex validatedLedger = xrplClient
					.ledger(LedgerRequestParams.builder().ledgerIndex(LedgerIndex.VALIDATED).build()).ledgerIndex()
					.orElseThrow(() -> new RuntimeException("LedgerIndex not available."));

			final UnsignedInteger lastLedgerSequence = UnsignedInteger
					.valueOf(validatedLedger.plus(UnsignedLong.valueOf(4)).unsignedLongValue().intValue()); // <--
																											// LastLedgerSequence
																											// is the
																											// current
																											// ledger
																											// index
																											// + 4
			final CurrencyAmount currencyAmount;
			if ("XRP".equals(paymentRequest.getCurrencyName())) {
				currencyAmount = XrpCurrencyAmount.ofXrp(new BigDecimal(amount));
			} else {
				currencyAmount = IssuedCurrencyAmount.builder().currency(paymentRequest.getCurrencyName())
						.issuer(Address.of(paymentRequest.getTrustlineIssuerClassicAddress())).value(amount).build();
			}

			AccountInfoResult fromAccount;
			try {
				fromAccount = getAccountInfo(fromClassicAddress);
			} catch (Exception e) {
				log.warn("Error fething account " + fromClassicAddress, e);
				return sendFSEPayment(paymentRequest, toClassicAddress, attempt);
			}
			Payment payment = Payment.builder().account(Address.of(fromClassicAddress)).amount(currencyAmount)
					.destination(Address.of(toClassicAddress)).sequence(fromAccount.accountData().sequence())
					.fee(openLedgerFee).signingPublicKey(paymentRequest.getFromSigningPublicKey())
					.lastLedgerSequence(lastLedgerSequence).build();

			if (destinationTag != null) {
				payment = Payment.builder().account(Address.of(fromClassicAddress))
						.amount(IssuedCurrencyAmount.builder().currency(paymentRequest.getCurrencyName())
								.issuer(Address.of(paymentRequest.getTrustlineIssuerClassicAddress())).value(amount)
								.build())
						.destination(Address.of(toClassicAddress)).sequence(fromAccount.accountData().sequence())
						.fee(openLedgerFee).signingPublicKey(paymentRequest.getFromSigningPublicKey())
						.lastLedgerSequence(lastLedgerSequence).destinationTag(UnsignedInteger.valueOf(destinationTag))
						.build();

			}

			// Construct a SignatureService to sign the Payment
			PrivateKey privateKey = PrivateKey.fromBase16EncodedPrivateKey(privateKeyStr);
			SignatureService signatureService = new SingleKeySignatureService(privateKey);

			final SignedTransaction<Payment> signedPayment;
			// Sign the Payment
			try {
				signedPayment = signatureService.sign(KeyMetadata.EMPTY, payment);
			} catch (EncodingFormatException e) {
				log.warn("Bad payment message. Probably invalid address " + payment, e);
				return toClassicAddress + " FAILED to pay:" + e.getMessage();
			}
			final SubmitResult<Transaction> submitResult = xrplClient.submit(signedPayment);

			log.info(submitResult.engineResultMessage() + "- payment to:" + toClassicAddress + " currency:"
					+ paymentRequest.getCurrencyName() + "  (amount:" + amount + ") (XRP fee:" + openLedgerFee.toXrp()
					+ ")");

			if ("tecDST_TAG_NEEDED".equals(submitResult.result()) && destinationTag == null) {
				paymentRequest.setDestinationTag("589");
				return sendFSEPayment(paymentRequest, toClassicAddress, attempt);
			}
			if ("tefPAST_SEQ".equals(submitResult.result()) || "telCAN_NOT_QUEUE".equals(submitResult.result())) {
				// retry if sequence already past
				return sendFSEPayment(paymentRequest, toClassicAddress, attempt);
			}
			if (!("tesSUCCESS".equals(submitResult.result()) || "terQUEUED".equals(submitResult.result()))) {
				log.warn("Payment FAILED " + submitResult.transactionResult());
				return submitResult.result();
			}
			final int attemptInThread = attempt;
			executor.submit(() -> waitForLedgerSuccess(submitResult, paymentRequest, toClassicAddress, signedPayment,
					lastLedgerSequence, fromAccount, attemptInThread));
			return submitResult.result();
		} catch (Exception e) {
			log.error("Error sending payment to address" + toClassicAddress, e);
			return "FAILED to pay " + toClassicAddress;
		}

	}

	private FeeResult waitForReasonableFee(XrplClient xrplClient) throws Exception {

		final FeeResult feeResult = xrplClient.fee();
		final XrpCurrencyAmount openLedgerFee = feeResult.drops().openLedgerFee();

		if (openLedgerFee.toXrp().compareTo(new BigDecimal(MAX_XRP_FEE_PER_TRANSACTION)) > 0) {
			log.warn("Waiting Fee is too high! " + openLedgerFee.toXrp());
			Thread.sleep(1000);
			return waitForReasonableFee(xrplClient);
		}

		return feeResult;
	}

	private void waitForLedgerSuccess(SubmitResult<Transaction> submitResult, FsePaymentRequest paymentRequest,
			String toClassicAddress, SignedTransaction<Payment> signedPayment, UnsignedInteger lastLedgerSequence,
			AccountInfoResult fromAccount, int attempt) {

		try {
			TransactionResult<Payment> transactionResult = null;

			int MAX_WAIT_LOOPS = 10;
			for (int i = 0; i < MAX_WAIT_LOOPS; i++) {
				Thread.sleep(4 * 1000);
				final LedgerIndex latestValidatedLedgerIndex = xrplClient
						.ledger(LedgerRequestParams.builder().ledgerIndex(LedgerIndex.VALIDATED).build()).ledgerIndex()
						.orElseThrow(() -> new RuntimeException("Ledger response did not contain a LedgerIndex."));

				try {
					transactionResult = xrplClient.transaction(TransactionRequestParams.of(signedPayment.hash()),
							Payment.class);
				} catch (Exception e) {
					log.info("{} try to find transaction for: {} : loopCount: {}", e.getMessage(), toClassicAddress, i);
					continue;
				}
				if (transactionResult.validated()) {
					log.info("Payment to {} was validated with result code {} on attempt {}", toClassicAddress,
							transactionResult.metadata().get().transactionResult(), attempt);
					return;
				} else {
					final boolean lastLedgerSequenceHasPassed = FluentCompareTo
							.is(latestValidatedLedgerIndex.unsignedLongValue())
							.greaterThan(UnsignedLong.valueOf(lastLedgerSequence.intValue()));
					if (lastLedgerSequenceHasPassed) {
						log.info("transactionExpired - LastLedgerSequence has passed. Last tx response: to {}  tx {}",
								toClassicAddress, transactionResult);
						if (attempt > 3) {
							log.error("Max attempts reached. Failing transaction to " + toClassicAddress);
							return;
						} else {
							log.info("Retrying to send payment.  Attempt {} to {}", attempt, toClassicAddress);

							sendFSEPayment(paymentRequest, toClassicAddress, attempt);
							return;
						}
					}
					if (i == MAX_WAIT_LOOPS) {
						log.info("Completely failed to determine success {} toClassicAddress: {} attempt: {}",
								transactionResult, toClassicAddress, attempt);
					}
				}
			}
		} catch (Exception e) {
			log.error("Error with payment transaction validation.", e);
		}
	}

}
