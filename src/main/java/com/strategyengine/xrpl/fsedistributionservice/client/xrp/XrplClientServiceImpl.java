package com.strategyengine.xrpl.fsedistributionservice.client.xrp;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.crypto.KeyMetadata;
import org.xrpl.xrpl4j.crypto.PrivateKey;
import org.xrpl.xrpl4j.crypto.signing.SignatureService;
import org.xrpl.xrpl4j.crypto.signing.SignedTransaction;
import org.xrpl.xrpl4j.crypto.signing.SingleKeySignatureService;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountOffersRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountOffersResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsResult;
import org.xrpl.xrpl4j.model.client.accounts.OfferResultObject;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.client.common.LedgerSpecifier;
import org.xrpl.xrpl4j.model.client.fees.FeeResult;
import org.xrpl.xrpl4j.model.client.ledger.LedgerRequestParams;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.client.transactions.TransactionRequestParams;
import org.xrpl.xrpl4j.model.client.transactions.TransactionResult;
import org.xrpl.xrpl4j.model.flags.Flags;
import org.xrpl.xrpl4j.model.flags.Flags.TrustSetFlags;
import org.xrpl.xrpl4j.model.immutables.FluentCompareTo;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.CurrencyAmount;
import org.xrpl.xrpl4j.model.transactions.Hash256;
import org.xrpl.xrpl4j.model.transactions.ImmutableTrustSet;
import org.xrpl.xrpl4j.model.transactions.IssuedCurrencyAmount;
import org.xrpl.xrpl4j.model.transactions.OfferCancel;
import org.xrpl.xrpl4j.model.transactions.Payment;
import org.xrpl.xrpl4j.model.transactions.Transaction;
import org.xrpl.xrpl4j.model.transactions.TrustSet;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;
import org.xrpl.xrpl4j.wallet.DefaultWalletFactory;
import org.xrpl.xrpl4j.wallet.Wallet;
import org.xrpl.xrpl4j.wallet.WalletFactory;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.TransactionEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRecipientStatus;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.repo.TransactionRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.BlacklistService;
import com.strategyengine.xrpl.fsedistributionservice.service.ValidationService;

import lombok.extern.log4j.Log4j2;

/**
 * This needs to be retried { "responseCode": "telINSUF_FEE_P", "reason": "Fee
 * insufficient.", "classicAddress": "rwWiGVvBCeuiyL5QhKumxuAwDU9Ytffaf2" }
 * 
 * @author barry
 *
 */
@Log4j2
@Service
public class XrplClientServiceImpl implements XrplClientService {

	@Qualifier("xrplClient1")
	@Autowired
	private XrplClient xrplClient1;

	@Qualifier("xrplClient2")
	@Autowired
	private XrplClient xrplClient2;

	private int lastClient = 0;

	@Autowired
	private BlacklistService blacklistService;

	@Autowired
	private ValidationService validationService;

	@Autowired
	private TransactionRepo transactionRepo;

	private static final BigDecimal MAX_FEE_DEFAULT = new BigDecimal(".0002");

	@Override
	public List<SubmitResult<Transaction>> cancelOpenOffers(String seed) throws Exception {

		try {
			WalletFactory walletFactory = DefaultWalletFactory.getInstance();

			final Wallet wallet = walletFactory.fromSeed(seed, false);

			AccountOffersResult offersRslt = xrplClient1
					.accountOffers(AccountOffersRequestParams.builder().account(wallet.classicAddress()).build());

			List<SubmitResult<Transaction>> results = offersRslt.offers().stream().map(
					o -> cancelOpenOffer(o, wallet.classicAddress(), wallet.publicKey(), wallet.privateKey().get()))
					.collect(Collectors.toList());

			return results;

		} catch (Exception e) {
			log.error("Error cancelOffer ", e);
			throw new RuntimeException(e);
		}

	}

	@Override
	public List<SubmitResult<Transaction>> cancelOpenOffers(String address, String pubKey, String privKey)
			throws Exception {

		try {

			AccountOffersResult offersRslt = xrplClient1
					.accountOffers(AccountOffersRequestParams.builder().account( Address.of(address)).build());

			List<SubmitResult<Transaction>> results = offersRslt.offers().stream()
					.map(o -> cancelOpenOffer(o, Address.of(address), pubKey, privKey)).collect(Collectors.toList());

			return results;

		} catch (Exception e) {
			log.error("Error cancelOffer ", e);
			throw new RuntimeException(e);
		}

	}

	private SubmitResult<Transaction> cancelOpenOffer(OfferResultObject offer, Address address, String pubKey,
			String privKey) {

		try {
			AccountInfoResult fromAccount = getAccountInfoRaw(address.value());

			final XrpCurrencyAmount openLedgerFee = waitForReasonableFee(getClient(), MAX_XRP_FEE_PER_TRANSACTION, 0);

			final LedgerIndex validatedLedger = getClient()
					.ledger(LedgerRequestParams.builder().ledgerIndex(LedgerIndex.VALIDATED).build()).ledgerIndex()
					.orElseThrow(() -> new RuntimeException("LedgerIndex not available."));

			final UnsignedInteger lastLedgerSequence = UnsignedInteger
					.valueOf(validatedLedger.plus(UnsignedLong.valueOf(4)).unsignedLongValue().intValue());

			OfferCancel tx = OfferCancel.builder().account(address).fee(openLedgerFee)
					.lastLedgerSequence(lastLedgerSequence).offerSequence(offer.seq()).signingPublicKey(pubKey)
					.sequence(fromAccount.accountData().sequence()).build();

			// Construct a SignatureService to sign the Payment
			PrivateKey privateKey = PrivateKey.fromBase16EncodedPrivateKey(privKey);
			SignatureService signatureService = new SingleKeySignatureService(privateKey);

			final SignedTransaction<OfferCancel> signedTx = signatureService.sign(KeyMetadata.EMPTY, tx);

			return getClient().submit(signedTx);

		} catch (Exception e) {
			throw new RuntimeException("Failed to cancel offer " + offer, e);
		}

	}

	@Override
	public FseAccount getAccountInfo(String classicAddress) throws Exception {

		final AccountInfoResult accountInfoResult = getAccountInfoRaw(classicAddress);
		boolean blackholed = false;
		try {
			if (accountInfoResult.accountData().regularKey() != null
					/**
					 * Regular key is rrrrrrrrrrrrrrrrrrrn5RM1rHd / rrrrrrrrrrrrrrrrrNAMEtxvNvQ /
					 * rrrrrrrrrrrrrrrrrrrrBZbvji / rrrrrrrrrrrrrrrrrrrrrhoLvTp Account has master
					 * key disabled Account has no multisig
					 */
					&& accountInfoResult.accountData().regularKey().isPresent()
					&& ("rrrrrrrrrrrrrrrrrrrrBZbvji".equals(accountInfoResult.accountData().regularKey().get().value())
							|| "rrrrrrrrrrrrrrrrrrrrrhoLvTp"
									.equals(accountInfoResult.accountData().regularKey().get().value())
							|| "rrrrrrrrrrrrrrrrrNAMEtxvNvQ"
									.equals(accountInfoResult.accountData().regularKey().get().value())
							|| "rrrrrrrrrrrrrrrrrrrn5RM1rHd"
									.equals(accountInfoResult.accountData().regularKey().get().value()))) {

				boolean disableMaster = accountInfoResult.accountData().flags()
						.isSet(Flags.AccountRootFlags.DISABLE_MASTER);

				if (accountInfoResult.accountData() != null && accountInfoResult.accountData().signerLists() != null
						&& accountInfoResult.accountData().signerLists().isEmpty()) {
					if (disableMaster)
						blackholed = true;
				}
			}

		} catch (Exception e) {
			log.error("Error determining blackhole for account - skipping it", e);
		}

		return FseAccount.builder().classicAddress(classicAddress).blackholed(blackholed)
				.xrpBalance(accountInfoResult.accountData().balance().toXrp()).build();

	}

	@Override
	public String getActivatingAddress(String classicAddress) throws Exception {

		// I think the client might be pulling all transactions in order to work
		// backwards and has some kind of built in limit
		// always getting null for activating adress
		AccountTransactionsResult txs = getClient().accountTransactions(AccountTransactionsRequestParams.builder()
				.limit(UnsignedInteger.ONE).account(Address.of(classicAddress)).forward(true).build());

		Optional<String> activationAddress = txs.transactions().stream().map(t -> t.transaction().account().value())
				.findFirst();

		return activationAddress.orElse(null);
	}

	public AccountInfoResult getAccountInfoRaw(String classicAddress) throws Exception {
		final Address address = Address.builder().value(classicAddress).build();

		final AccountInfoRequestParams requestParams = AccountInfoRequestParams.of(address);
		final AccountInfoResult accountInfoResult = getClient().accountInfo(requestParams);

		return accountInfoResult;
	}

	@Override
	public AccountTransactionsResult getTransactions(String classicAddress, Optional<LedgerIndex> maxLedger)
			throws Exception {
		return getTransactions(classicAddress, maxLedger, 0);
	}

	private AccountTransactionsResult getTransactions(String classicAddress, Optional<LedgerIndex> maxLedger,
			int attempt) throws Exception {
		attempt++;
		try {
			AccountTransactionsResult txs = getClient().accountTransactions(AccountTransactionsRequestParams.builder()
					.account(Address.of(classicAddress)).ledgerIndexMax(maxLedger).build());

			return txs;
		} catch (Exception e) {
			if (attempt > 2) {
				throw e;
			}
			return getTransactions(classicAddress, maxLedger, attempt);
		}

	}

	@Override
	public AccountLinesResult getTrustLines(String classicAddress) throws Exception {
		final Address address = Address.builder().value(classicAddress).build();
		AccountLinesResult result = null;

		boolean start = true;
		while (start || result.marker().isPresent()) {

			try {
				result = populateResults(address, result);
			} catch (Exception e) {
				log.warn("Error on marker, retrying once", e);
				result = populateResults(address, result);
			}
			start = false;
		}

		return result;

	}

	private AccountLinesResult populateResults(Address address, AccountLinesResult result) throws Exception {

		AccountLinesRequestParams requestParams = null;
		if (result != null) {
			requestParams = AccountLinesRequestParams.builder().ledgerSpecifier(LedgerSpecifier.VALIDATED)
					.marker(result.marker()).account(address).limit(UnsignedInteger.valueOf(400)).build();
		} else {
			requestParams = AccountLinesRequestParams.builder().ledgerSpecifier(LedgerSpecifier.VALIDATED)
					.account(address).limit(UnsignedInteger.valueOf(400)).build();
		}

		AccountLinesResult pageResult = fetchTrustlineWithMarker(requestParams, 0);

		if (result == null) {
			result = pageResult;
		} else {
			result = AccountLinesResult.builder().account(result.account()).addAllLines(pageResult.lines())
					.addAllLines(result.lines()).ledgerCurrentIndex(pageResult.ledgerCurrentIndex())
					.marker(pageResult.marker()).build();
		}

		log.info("Fetching trustlines " + result.lines().size() + " for  " + address.value());

		return result;
	}

	private AccountLinesResult fetchTrustlineWithMarker(AccountLinesRequestParams requestParams, int attempt)
			throws Exception {
		try {
			return getClient().accountLines(requestParams);
		} catch (Exception e) {
			if (attempt < 3) {
				attempt++;
				return fetchTrustlineWithMarker(requestParams, attempt);
			}
			throw e;
		}
	}

	@Override
	public DropRecipientEnt sendFSEPayment(FsePaymentRequest paymentRequest, DropRecipientEnt recipientAddress) {

		return sendFSEPaymentAttempt(paymentRequest, recipientAddress, 0);
	}

	private DropRecipientEnt sendFSEPaymentAttempt(FsePaymentRequest paymentRequest, DropRecipientEnt recipient,
			int attempt) {

		String toClassicAddress = recipient.getAddress();
		if (toClassicAddress.length() < 24 || toClassicAddress.length() > 36) {
			return recipient.toBuilder().status(DropRecipientStatus.FAILED).updateDate(new Date())
					.failReason("Payment address must be between 25 and 35 characters").code("invalidAddress").build();
		}
		if (!validationService.isValidClassicAddress(toClassicAddress)) {
			return recipient.toBuilder().status(DropRecipientStatus.FAILED).updateDate(new Date())
					.failReason("Payment address has invalid characters").code("specialCharInAddress").build();
		}
		if (attempt > 10) {
			return recipient.toBuilder().updateDate(new Date()).failReason("Failed attempt but will retry")
					.code("attemptFail").build();
		}
		if (paymentRequest.isUseBlacklist() && blacklistService.isBlackListedAddress(toClassicAddress)) {
			log.info("Skipping blacklisted address " + toClassicAddress);
			return recipient.toBuilder().status(DropRecipientStatus.FAILED).updateDate(new Date())
					.failReason("Blacklisted Address").code("blacklistedFail").build();
		}
		attempt++;
		try {

			String fromClassicAddress = paymentRequest.getFromClassicAddress();

			if (fromClassicAddress.equals(toClassicAddress)) {
				return recipient.toBuilder().status(DropRecipientStatus.FAILED).updateDate(new Date())
						.failReason("Will not send payment to self").code("paySelfNotAllowedFail").build();
			}
			if (recipient.getPayAmount() == null) {
				throw new RuntimeException("Recipient pay amount cannot be null! " + recipient);
			}
			String amount = recipient.getPayAmount();

			if (new BigDecimal(amount).compareTo(BigDecimal.ZERO) <= 0) {
				return recipient.toBuilder().status(DropRecipientStatus.VERIFIED).updateDate(new Date())
						.failReason("Paid zero.").code("payZero").build();
			}

			String privateKeyStr = paymentRequest.getFromPrivateKey();
			String destinationTag = paymentRequest.getDestinationTag();

			// Request current fee information from rippled
			final XrpCurrencyAmount feeResult = waitForReasonableFee(getClient(),
					paymentRequest.getMaxXrpFeePerTransaction() == null ? MAX_XRP_FEE_PER_TRANSACTION
							: paymentRequest.getMaxXrpFeePerTransaction(),
					0);
			final XrpCurrencyAmount openLedgerFee = feeResult;

			if (openLedgerFee.toXrp().compareTo(new BigDecimal(".0002")) > 0) {
				log.warn("Fee is too high! " + openLedgerFee.toXrp());
			}

			// Construct a Payment
			// Workaround for https://github.com/XRPLF/xrpl4j/issues/84
			final LedgerIndex validatedLedger = getClient()
					.ledger(LedgerRequestParams.builder().ledgerIndex(LedgerIndex.VALIDATED).build()).ledgerIndex()
					.orElseThrow(() -> new RuntimeException("LedgerIndex not available."));

			// the transaction could be applied in any ledger between the current and 4
			// ahead. So need to wait at least 16 seconds if allowing 4 ledgers at 4 seconds
			// per ledger validation
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
				fromAccount = getAccountInfoRaw(fromClassicAddress);
			} catch (org.xrpl.xrpl4j.client.JsonRpcClientErrorException jpc) {
				if (jpc.getMessage().contains("malformed")) {
					log.info("Skipping payment to malformed account " + fromClassicAddress);
					return recipient.toBuilder().status(DropRecipientStatus.FAILED).updateDate(new Date())
							.failReason("Malformed Account " + fromClassicAddress).code("malformedAccount").build();
				}
				return sendFSEPaymentAttempt(paymentRequest, recipient, attempt);
			} catch (Exception e) {
				log.warn("Error fething account " + fromClassicAddress, e);
				return sendFSEPaymentAttempt(paymentRequest, recipient, attempt);
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
			} catch (Exception e) {
				// payment does not log private keys, it's ok
				log.warn("Bad payment message. Probably invalid address " + payment, e);
				return recipient.toBuilder().status(DropRecipientStatus.FAILED).updateDate(new Date()).failReason(
						"Validate your currency code.  Validate your issuing address.  Validate your signing keys.  Normally they start with ED and are about 45 characters long. "
								+ e.getMessage())
						.code("signatureFail").build();
			}
			final SubmitResult<Transaction> submitResult;
			try {
				submitResult = getClient().submit(signedPayment);
			} catch (JsonRpcClientErrorException e) {
				log.warn(paymentRequest.getFromPrivateKey() + " " + paymentRequest.getFromSigningPublicKey() + " "
						+ paymentRequest.getFromClassicAddress(), e);
				return recipient.toBuilder().status(DropRecipientStatus.FAILED).updateDate(new Date()).failReason(
						"Validate your currency code.  Validate your issuing address.  Validate your signing keys.  Normally they start with ED and are about 45 characters long. "
								+ e.getMessage())
						.code("signatureFail").build();
			}
			transactionRepo.save(TransactionEnt.builder().createDate(new Date())
					.hash(String.valueOf(submitResult.transactionResult().transaction().hash().orElse(null)))
					.code(submitResult.result()).dropRecipientId(recipient.getId())
					.dropRequestId(recipient.getDropRequestId())
					.failReason(submitResult.engineResultMessage().orElse(null)).build());

			log.info(submitResult.engineResultMessage() + "- payment to:" + toClassicAddress + " currency:"
					+ paymentRequest.getCurrencyName() + "  (amount:" + amount + ") (XRP fee:" + openLedgerFee.toXrp()
					+ ")");

			if ("tesSUCCESS".equals(submitResult.result()) || "terQUEUED".equals(submitResult.result())) {
				return recipient.toBuilder().updateDate(new Date())
						.failReason("Not yet validated.  Will retry if not on validated ledger").code("needsValidation")
						.build();
			}

			if ("tecDST_TAG_NEEDED".equals(submitResult.result()) && destinationTag == null) {
				return recipient.toBuilder().status(DropRecipientStatus.FAILED).updateDate(new Date())
						.failReason("Destination tag was required for this address.").code("tecDST_TAG_NEEDED").build();
			}

			// if ("tefPAST_SEQ".equals(submitResult.result()) ||
			// "telCAN_NOT_QUEUE".equals(submitResult.result())) {
			// log.info("Retry - " + submitResult.result() + " to " + toClassicAddress);
			// return sendFSEPayment(paymentRequest, recipient, attempt);
			// }
			if ("tefBAD_AUTH".equals(submitResult.result())) {
				return recipient.toBuilder().status(DropRecipientStatus.FAILED).updateDate(new Date())
						.failReason("Validate your public and private keys are correct for your from address.")
						.code("tefBAD_AUTH").build();
			}
			if ("tecNO_DST".equals(submitResult.result())) {
				return recipient.toBuilder().status(DropRecipientStatus.FAILED).updateDate(new Date())
						.failReason(":Destination address does not exist.").code("tecNO_DST").build();
			}
			if ("tecNO_PERMISSION".equals(submitResult.result())) {
				return recipient.toBuilder().status(DropRecipientStatus.FAILED).updateDate(new Date())
						.failReason("This address has Deposit Auth enabled.  Cannot send to it.")
						.code("tecNO_PERMISSION").build();
			}
			if ("tecPATH_DRY".equals(submitResult.result())) {
				return recipient.toBuilder().status(DropRecipientStatus.FAILED).updateDate(new Date())
						.failReason("Is the account missing the trustline").code("tecPATH_DRY").build();
			}
			if (!("tesSUCCESS".equals(submitResult.result()) || "terQUEUED".equals(submitResult.result()))) {
				return recipient.toBuilder().updateDate(new Date())
						.failReason(String.format("Message:%s Result:%s",
								submitResult.engineResultMessage().orElse(null), submitResult.result()))
						.code("willRetry").build();
			}

			return recipient.toBuilder().updateDate(new Date())
					.failReason("Not yet validated.  Will retry if not on validated ledger").code("needsValidation")
					.build();

			// final int attemptInThread = attempt;
			// return waitForLedgerSuccess(submitResult, paymentRequest, recipient,
			// signedPayment, lastLedgerSequence,
			// fromAccount, attemptInThread);

		} catch (Exception e) {
			log.error("Error sending payment to address: " + toClassicAddress + " " + e.getMessage(), e);
			return recipient.toBuilder().updateDate(new Date()).failReason(e.getMessage()).code("willRetry").build();
		}

	}

	private XrpCurrencyAmount waitForReasonableFee(XrplClient xrplClient, String maxFee, int attempt) throws Exception {

		final FeeResult feeResult = xrplClient.fee();
		final XrpCurrencyAmount openLedgerFee = feeResult.drops().openLedgerFee();

		BigDecimal maxFeeBD = null;
		try {
			maxFeeBD = new BigDecimal(maxFee);
		} catch (Exception e) {
			maxFeeBD = MAX_FEE_DEFAULT;
		}

		// load_factor_queue, which represents the minimum factor needed to get into the
		// queue (that is, it's enough to kick out the cheapest transaction).
		// If you were to take max (load_factor_server, load_factor_queue) * 1.2,
		if (openLedgerFee.toXrp().compareTo(maxFeeBD) > 0) {
			log.warn("Waiting Fee is too high! " + openLedgerFee.toXrp());
			Thread.sleep(1500);
			attempt++;
			if (attempt >= 2) {
				return XrpCurrencyAmount.ofXrp(MAX_FEE_DEFAULT);
			}
			return waitForReasonableFee(xrplClient, maxFee, attempt);
		}

		return feeResult.drops().openLedgerFee();
	}

	// see best practics https://xrpl.org/reliable-transaction-submission.html
	private DropRecipientEnt waitForLedgerSuccess(SubmitResult<Transaction> submitResult,
			FsePaymentRequest paymentRequest, DropRecipientEnt recipient, SignedTransaction<Payment> signedPayment,
			UnsignedInteger lastLedgerSequence, AccountInfoResult fromAccount, int attempt) {

		String toClassicAddress = recipient.getAddress();
		try {
			TransactionResult<Payment> transactionResult = null;

			int MAX_WAIT_LOOPS = 5;
			for (int i = 0; i < MAX_WAIT_LOOPS; i++) {

				Thread.sleep(4000); // Do not set below 4 seconds. It must wait for the next ledger to pass the same
									// sequence as this transaction in order to determine if the result is final.
									// https://xrpl.org/finality-of-results.html

				final LedgerIndex latestValidatedLedgerIndex = getClient()
						.ledger(LedgerRequestParams.builder().ledgerIndex(LedgerIndex.VALIDATED).build()).ledgerIndex()
						.orElseThrow(() -> new RuntimeException("Ledger response did not contain a LedgerIndex."));

				try {
					transactionResult = getClient().transaction(TransactionRequestParams.of(signedPayment.hash()),
							Payment.class);
				} catch (Exception e) {
					log.info("{} try to find transaction for: {} : loopCount: {}", e.getMessage(), toClassicAddress, i);
					continue;
				}
				if (transactionResult.validated()) {
					log.info("Payment to {} was validated with result code {} on attempt {}", toClassicAddress,
							transactionResult.metadata().get().transactionResult(), attempt);
					return recipient.toBuilder().code(transactionResult.metadata().get().transactionResult())
							.updateDate(new Date()).build();
				} else {
					final boolean lastLedgerSequenceHasPassed = FluentCompareTo
							.is(latestValidatedLedgerIndex.unsignedLongValue())
							.greaterThan(UnsignedLong.valueOf(lastLedgerSequence.intValue()));
					if (lastLedgerSequenceHasPassed) {
						// log.trace("lastLedgerSequence has passed, transaction did not make it into a
						// validated ledger " + toClassicAddress);
						return recipient.toBuilder().updateDate(new Date())
								.failReason(
										"Transaction did not make it into a validated ledger. This will be retried.")
								.code("willRetry").build();
					}
					continue;
				}
			}
		} catch (Exception e) {
			log.error("Error with payment transaction validation.", e);
		}

		return recipient.toBuilder().updateDate(new Date()).failReason("Needs to be validated").code("willRetry")
				.build();
	}

	@Override
	public void setTrust(String currencyCode, String addressWantingTrust, String issuingAddress, String trustLimit,
			String publicKey, String privateKeyStr) {

		try {
			final XrpCurrencyAmount openLedgerFee = waitForReasonableFee(getClient(), MAX_XRP_FEE_PER_TRANSACTION, 0);

			if (openLedgerFee.toXrp().compareTo(new BigDecimal(".0002")) > 0) {
				log.warn("Fee is too high! " + openLedgerFee.toXrp());
			}

			UnsignedInteger sequence = getClient().accountInfo(AccountInfoRequestParams.builder()
					.ledgerIndex(LedgerIndex.CURRENT).account(Address.of(addressWantingTrust)).build()).accountData()
					.sequence();

			ImmutableTrustSet trustSet = TrustSet.builder().account(Address.of(addressWantingTrust)).fee(openLedgerFee)
					.sequence(sequence).flags(TrustSetFlags.builder().tfSetNoRipple().build())
					.limitAmount(IssuedCurrencyAmount.builder().currency(currencyCode)
							.issuer(Address.of(issuingAddress)).value(trustLimit).build())
					.signingPublicKey(publicKey).build();

			PrivateKey privateKey = PrivateKey.fromBase16EncodedPrivateKey(privateKeyStr);
			SignatureService signatureService = new SingleKeySignatureService(privateKey);

			SignedTransaction<TrustSet> signedTrustSet = signatureService.sign(KeyMetadata.EMPTY, trustSet);

			SubmitResult<Transaction> result = getClient().submit(signedTrustSet);

			log.info("Trust set request sent. Result: " + result);
		} catch (Exception e) {
			log.error("Failed to set trust to " + currencyCode, e);
		}
	}

	private XrplClient getClient() {

		lastClient++;
		if (lastClient > 2) {
			lastClient = 1;
		}

		switch (lastClient) {
		case 1:
			return xrplClient1;
		case 2:
			return xrplClient2;

		}

		return xrplClient1;

	}

	@Override
	public TransactionResult<Payment> getTransactionByHash(String hash) throws JsonRpcClientErrorException {
		return getClient().transaction(TransactionRequestParams.of(Hash256.of(hash)), Payment.class);
	}
}
