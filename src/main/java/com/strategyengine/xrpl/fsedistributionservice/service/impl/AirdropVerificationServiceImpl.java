package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.model.client.transactions.TransactionResult;
import org.xrpl.xrpl4j.model.transactions.Payment;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.entity.CancelDropRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.TransactionEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRecipientStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRequestStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropType;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseSort;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.repo.CancelDropRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropRecipientRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.TransactionRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropVerificationService;
import com.strategyengine.xrpl.fsedistributionservice.service.ConfigService;
import com.strategyengine.xrpl.fsedistributionservice.service.EmailService;
import com.strategyengine.xrpl.fsedistributionservice.service.PaymentService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class AirdropVerificationServiceImpl implements AirdropVerificationService {

	@VisibleForTesting
	@Autowired
	protected DropRecipientRepo dropRecipientRepo;

	@VisibleForTesting
	@Autowired
	protected PaymentRequestRepo paymentRequestRepo;

	@VisibleForTesting
	@Autowired
	protected TransactionRepo transactionRepo;

	@VisibleForTesting
	@Autowired
	protected TransactionService transactionService;

	@VisibleForTesting
	@Autowired
	protected CancelDropRequestRepo cancelDropRequestRepo;

	@VisibleForTesting
	@Autowired
	protected ConfigService configService;

	@VisibleForTesting
	@Autowired
	protected PaymentService paymentService;

	@VisibleForTesting
	@Autowired
	protected XrplService xrplService;

	@VisibleForTesting
	@Autowired
	protected EmailService emailService;

	private static int MINUTES_WAIT_FOR_NEW_RUN = 5;

	// if sending to different address, then service fee is doubled
	public static final String SERVICE_FEE_INTERVAL_UNVERIFIED = "SERVICE_FEE_INTERVAL_UNVERIFIED";
	public static final String DEFAULT_SERVICE_FEE_PER_INTERVAL_UNVERFIED = "DEFAULT_SERVICE_FEE_PER_INTERVAL_UNVERFIED";

	public static final String SERVICE_FEE_INTERVAL_VERIFIED = "SERVICE_FEE_INTERVAL_VERIFIED";
	public static final String DEFAULT_SERVICE_FEE_PER_INTERVAL_VERFIED = "DEFAULT_SERVICE_FEE_PER_INTERVAL_VERFIED";
	public static final int REMAINING_TO_TAKE_PRIORITY = 75;

	private long MIN_DROP_REQUEST_ID_TO_COLLECT_FEES = 4710;

	public static final DropRecipientEnt SERVICE_FEE_ADDRESS = DropRecipientEnt.builder()
			.address("rQxiuuzj2p7nU54wdtGe8gaas91UeWRBx").build();

	public static final DropRecipientEnt BURN_ISSUER_FSE_ADDRESS = DropRecipientEnt.builder()
			.address("rs1MKY54miDtMFEGyNuPd3BLsXauFZUSrj").build();

	public static final String FSE_ISSUING_ADDRESS = "rs1MKY54miDtMFEGyNuPd3BLsXauFZUSrj";

	private String environment = System.getenv("ENV");

	@Transactional
	@Override
	public PaymentRequestEnt verifyDropComplete(PaymentRequestEnt paymentRequestInitial,
			List<FseTransaction> transactions, boolean releaseLock) {

		try {

			if (cancelDropRequestRepo.exists(
					Example.of(CancelDropRequestEnt.builder().dropRequestId(paymentRequestInitial.getId()).build()))) {
				try {
					paymentRequestInitial.setFailReason(AirDropRunnerImpl.REASON_CANCEL_BY_USER);
					paymentRequestInitial.setStatus(DropRequestStatus.REJECTED);
					paymentRequestInitial.setFromPrivateKey(null);
					paymentRequestInitial.setFromSigningPublicKey(null);
					return paymentRequestRepo.save(paymentRequestInitial);

				} catch (Exception e) {
					throw new RuntimeException("Error Job Cancelled by user " + paymentRequestInitial, e);
				}
			}

			PaymentRequestEnt paymentRequest = collectFees(paymentRequestInitial,
					dropRecipientRepo.findAll(Example.of(DropRecipientEnt.builder().status(DropRecipientStatus.VERIFIED)
							.dropRequestId(paymentRequestInitial.getId()).build())).size());

			if (MIN_DROP_REQUEST_ID_TO_COLLECT_FEES < paymentRequest.getId()
					&& (paymentRequest.getFeesPaid() == null || Double.valueOf(paymentRequest.getFeesPaid()) == 0)) {
				throw new RuntimeException("Failed to collect any fee.  Will not start airdrop");
			}

			List<DropRecipientEnt> recipientsToValidate = dropRecipientRepo.findAll(Example.of(DropRecipientEnt
					.builder().status(DropRecipientStatus.QUEUED).dropRequestId(paymentRequest.getId()).build()));

			recipientsToValidate.addAll(dropRecipientRepo.findAll(Example.of(DropRecipientEnt.builder()
					.status(DropRecipientStatus.SENDING).dropRequestId(paymentRequest.getId()).build())));

			List<DropRecipientEnt> shouldHaveRecievedDropButDidNot = new ArrayList<>();
			if (!recipientsToValidate.isEmpty()) {

				Map<String, List<FseTransaction>> airdDropByToAddress = getTransactionMap(paymentRequest, transactions);

				List<TransactionEnt> transactionHashes = transactionRepo
						.findAll(Example.of(TransactionEnt.builder().dropRequestId(paymentRequest.getId()).build()));

				log.info("Found transaction hashes to validate size - " + transactionHashes.size());
				shouldHaveRecievedDropButDidNot
						.addAll(recipientsToValidate.stream()
								.filter(t -> !didReceiveDrop(t, airdDropByToAddress, transactionHashes.stream().filter(
										p -> p.getDropRecipientId() != null && p.getDropRecipientId().equals(t.getId()))
										.collect(Collectors.toList())))
								.collect(Collectors.toList()));

				// remove unvalidated so we just have verified payed recipients
				recipientsToValidate.removeAll(shouldHaveRecievedDropButDidNot);
				// mark verified
				recipientsToValidate.stream().forEach(r -> {
					r.setStatus(DropRecipientStatus.VERIFIED);
					r.setUpdateDate(now());
					updateReason(r, airdDropByToAddress);
				});

				dropRecipientRepo.saveAll(recipientsToValidate);

				shouldHaveRecievedDropButDidNot.stream().filter(r -> this.didFail(r, airdDropByToAddress))
						.forEach(r -> dropRecipientRepo.save(r.toBuilder().status(DropRecipientStatus.FAILED).build()));

			}

			final PaymentRequestEnt finalizedPaymentRequest = paymentRequestRepo.save(paymentRequest.toBuilder()
					.updateDate(new Date()).lockUuid(releaseLock ? null : paymentRequest.getLockUuid())
					.status(DropRequestStatus.IN_PROGRESS).build());

			PaymentRequestEnt finalizedFeeCollected = collectFees(finalizedPaymentRequest,
					dropRecipientRepo.findAll(Example.of(DropRecipientEnt.builder().status(DropRecipientStatus.VERIFIED)
							.dropRequestId(paymentRequest.getId()).build())).size());

			log.info("Finished Airdrop verification for UUID: {} paymentRequest ID: {}",
					finalizedFeeCollected.getLockUuid(), finalizedFeeCollected.getId());

			if (shouldHaveRecievedDropButDidNot.isEmpty()) {
				// if this is empty then the drop is finally complete
				log.info("Airdrop marked COMPLETE, wiping keys id:{} currency:{}", finalizedFeeCollected.getId(),
						finalizedFeeCollected.getCurrencyName());

				sendCompleteEmail(finalizedFeeCollected);

				return paymentRequestRepo.save(finalizedFeeCollected.toBuilder().updateDate(new Date()).lockUuid(null)
						.fromPrivateKey(null).fromSigningPublicKey(null).status(DropRequestStatus.COMPLETE).build());
			}

			Optional<FseTrustLine> trustLineForFromAddress = xrplService
					.getTrustLines(finalizedFeeCollected.getFromClassicAddress(), FseSort.OLD).stream()
					.filter(t -> t.getCurrency().equals(finalizedFeeCollected.getCurrencyName())
							|| t.getCurrency().equals(finalizedFeeCollected.getCurrencyNameForProcess()))
					.findAny();

			if (trustLineForFromAddress.isPresent()) {
				if (new BigDecimal(trustLineForFromAddress.get().getBalance())
						.compareTo(new BigDecimal(finalizedFeeCollected.getAmount())) == -1) {
					// from address ran out of tokens to give
					log.info("Airdrop marked COMPLETE, wiping keys id:{} currency:{}", finalizedFeeCollected.getId(),
							finalizedFeeCollected.getCurrencyName());

					sendCompleteEmail(finalizedFeeCollected);
					return paymentRequestRepo.save(finalizedFeeCollected.toBuilder().updateDate(new Date())
							.lockUuid(null).status(DropRequestStatus.COMPLETE)
							.failReason("Address does not have enough tokens to send. "
									+ finalizedFeeCollected.getFromClassicAddress())
							.build());
				}
			}

			return finalizedFeeCollected;

		} catch (

		Exception e) {
			throw new RuntimeException("Error verifying drop for payment request id:" + paymentRequestInitial, e);
		}
	}

	private void sendCompleteEmail(PaymentRequestEnt p) {
		emailService.sendEmail(p.getContactEmail(), "Airdrop Complete",
				"Airdrop has completed.  Click here for details - <a href='https://strategyengine.one/#/airdropdetails?dropRequestId="
						+ p.getId() + "'>Airdrop Details</a>");

	}

	// NEVER CACHE THIS. USED FOR DROP VERIFICATION
	@Override
	public Map<String, List<FseTransaction>> getTransactionMap(PaymentRequestEnt paymentRequest,
			List<FseTransaction> transactions) {

		Map<String, List<FseTransaction>> airdDropByToAddress = new HashMap<String, List<FseTransaction>>();
		for (FseTransaction t : transactions) {
			if (t.getAmount() != null && (paymentRequest.getTrustlineIssuerClassicAddress().equals(t.getIssuerAddress())
					|| ("XRP".equals(paymentRequest.getCurrencyName()) && "XRP".equals(t.getCurrency())))) {

				List<FseTransaction> transactionsForAddress = airdDropByToAddress.get(t.getToAddress());
				if (transactionsForAddress == null) {
					transactionsForAddress = new ArrayList<FseTransaction>();
				}
				transactionsForAddress.add(t);
				airdDropByToAddress.put(t.getToAddress(), transactionsForAddress);
			}
		}

		return airdDropByToAddress;
	}

	protected boolean amountsEqual(BigDecimal a, String b) {
		return a.stripTrailingZeros().equals(new BigDecimal(b).stripTrailingZeros());
	}

	@Override
	public boolean didReceiveDrop(DropRecipientEnt recipient, Map<String, List<FseTransaction>> airdropByToAddress,
			List<TransactionEnt> transactionHashes) {

		List<FseTransaction> transactions = airdropByToAddress.get(recipient.getAddress());

		boolean paymentFound = transactions != null && transactions.stream()
				.filter(t -> "tesSUCCESS".equals(t.getResultCode())
						&& t.getAmount().compareTo(new BigDecimal(recipient.getPayAmount())) == 0
						&& TransactionHistoryServiceImpl.TRANSACTION_TYPE_PAYMENT.equals(t.getTransactionType()))
				.findAny().isPresent();

		if (!paymentFound && !transactionHashes.isEmpty()) {
			// TODO move up the fetching of transactionHashes for drop request id after it
			// is being populated for all drops
			// List<TransactionEnt> transactionHashes =
			// transactionRepo.findAll(Example.of(TransactionEnt.builder().dropRecipientId(recipient.getId()).build()));
			List<TransactionResult<Payment>> payments = transactionService
					.getXrplTransactionsByHashes(transactionHashes);
			Optional<TransactionResult<Payment>> paid = payments.stream().filter(p -> p.metadata().isPresent()
					&& "tesSUCCESS".equals(p.metadata().get().transactionResult()) && p.validated()).findAny();
			if (paid.isPresent()) {
				// found a payment, but did not find the payment with the transaction history
				log.warn("Payment was found but did not find the payment with transaction history " + paid);
				return true;
			}

		}

		return paymentFound;
	}

	// TODO FAIL transactions immediately
	private boolean didFail(DropRecipientEnt recipient, Map<String, List<FseTransaction>> airdropByToAddress) {

		List<FseTransaction> transactions = airdropByToAddress.get(recipient.getAddress());

		return transactions != null && transactions.stream()
				.filter(t -> "tecPATH_DRY".equals(t.getResultCode())
						&& TransactionHistoryServiceImpl.TRANSACTION_TYPE_PAYMENT.equals(t.getTransactionType()))
				.findAny().isPresent();
	}

	// handles populating dry path for verified payments
	private void updateReason(DropRecipientEnt recipient, Map<String, List<FseTransaction>> airdropByToAddress) {

		List<FseTransaction> transactions = airdropByToAddress.get(recipient.getAddress());

		if (transactions != null && !transactions.isEmpty()) {
			recipient.setFailReason(transactions.get(0).getResultCode());
		}

	}

	@Override
	public String lockPaymentRequest() {

		String uuid = UUID.randomUUID().toString();
		// try to flag one or more payment requests to start work on it
		paymentRequestRepo.updateUuid(uuid);

		return uuid;
	}

	@Override
	public @Nullable PaymentRequestEnt getPaymentRequestToProcess(String uuid) {

		List<PaymentRequestEnt> paymentRequests = paymentRequestRepo.findAll(
				Example.of(PaymentRequestEnt.builder().lockUuid(uuid).build()), Sort.by("dropType", "updateDate"));

		if (paymentRequests.isEmpty()) {
			return null;
		}

		// payment request to process
		PaymentRequestEnt paymentRequestEnt = null;
		for (PaymentRequestEnt p : paymentRequests) {

			if (p.getStartTime() != null && p.getStartTime().after(new Date())) {
				// not scheduled to start yet
				continue;
			}

			if (paymentRequestUpdatedTooRecently(p)) {
				continue;
			}

			if (!(DropRequestStatus.QUEUED.equals(p.getStatus())
					|| DropRequestStatus.IN_PROGRESS.equals(p.getStatus()))) {
				// do not run drops on scheduled, rejected, complete status
				continue;
			}

			// if there are less than 50 to pay, then finish it
			long remaining = dropRecipientRepo.count(Example.of(
					DropRecipientEnt.builder().dropRequestId(p.getId()).status(DropRecipientStatus.QUEUED).build()));
			if (remaining < REMAINING_TO_TAKE_PRIORITY) {
				paymentRequestEnt = p;
				break;
			}
		}

		if (paymentRequestEnt == null) {
			paymentRequestEnt = paymentRequests.get(0);
		}

		paymentRequests.remove(paymentRequestEnt);

		// clear out uuid on any other payment requests so they can be picked up by
		// another container
		paymentRequests.stream().forEach(p -> p.setLockUuid(null));
		paymentRequestRepo.saveAll(paymentRequests);

		if (paymentRequestUpdatedTooRecently(paymentRequestEnt)) {
			if (paymentRequestEnt != null) {
				paymentRequestEnt.setLockUuid(null);
				paymentRequestRepo.save(paymentRequestEnt);

			}
			return null;
		}
		log.info("Found airdrop to run " + paymentRequestEnt);

		return paymentRequestRepo
				.save(paymentRequestEnt.toBuilder().environment(environment).updateDate(new Date()).build());
	}

	private boolean paymentRequestUpdatedTooRecently(PaymentRequestEnt p) {
		long millisSinceUpdate = System.currentTimeMillis() - p.getUpdateDate().getTime();

		long minutesSinceUpdate = millisSinceUpdate / 1000 / 60;

		return minutesSinceUpdate < MINUTES_WAIT_FOR_NEW_RUN;

	}

	protected PaymentRequestEnt collectFees(PaymentRequestEnt p, int totalVerifiedRecipients) {

		if (p.getId() <= MIN_DROP_REQUEST_ID_TO_COLLECT_FEES) {
			// don't charge until we can switch over to FSE payments
			return p;

		}

		BigDecimal totalFeesCollected = p.getFeesPaid() == null ? BigDecimal.ZERO
				: new BigDecimal(p.getFeesPaid()).setScale(2, RoundingMode.DOWN);

		boolean isCrossCurrencySnapshotDrop = isCrossSnapshotDrop(p);

		BigDecimal totalFeeRequiredForSize = getTotalFeeRequiredForSize(totalVerifiedRecipients, p.getDropType(),
				isCrossCurrencySnapshotDrop);

		BigDecimal remainingToCollect = totalFeeRequiredForSize.subtract(totalFeesCollected).setScale(2,
				RoundingMode.DOWN);

		log.info(String.format(
				"dropId:%s totalVerifiedRecipients:%s totalFeesCollected:%s totalFeeRequiredForSize:%s remainingToCollect:%s "
						+ "currency:%s issuer:%s snapCurrency:%s snapIssuer:%s",
				p.getId(), totalVerifiedRecipients, totalFeesCollected, totalFeeRequiredForSize, remainingToCollect,
				p.getCurrencyName(), p.getTrustlineIssuerClassicAddress(), p.getSnapshotCurrencyName(),
				p.getSnapshotTrustlineIssuerClassicAddress()));

		if (totalFeesCollected.stripTrailingZeros().equals(BigDecimal.ZERO)
				&& remainingToCollect.stripTrailingZeros().equals(BigDecimal.ZERO)) {
			double serviceFee = isVerifiedDropType(p.getDropType())
					? configService.getDouble(DEFAULT_SERVICE_FEE_PER_INTERVAL_VERFIED)
					: configService.getDouble(DEFAULT_SERVICE_FEE_PER_INTERVAL_UNVERFIED);
			// if they never paid anything, then make them pay .1 to begin
			remainingToCollect = BigDecimal.valueOf(serviceFee).setScale(2, RoundingMode.DOWN);
		}

		if (remainingToCollect.compareTo(BigDecimal.ZERO) > 0) {

			// BigDecimal feePortionBurn =
			// remainingToCollect.divide(BigDecimal.valueOf(2l)); //half to Ardy, half to
			// burn
			// BigDecimal feePortionArdy =
			// remainingToCollect.divide(BigDecimal.valueOf(2l)); //half to Ardy, half to
			// burn

			BigDecimal feePortionBurn = remainingToCollect.multiply(new BigDecimal(".001")); // 90% burn; changed only burn 1%
			BigDecimal feePortionArdy = remainingToCollect.multiply(new BigDecimal(".991")); // 10% Ardy

			log.info(String.format("dropid:%s feePortionBurn:%s feePortionArdy:%s", p.getId(), feePortionBurn,
					feePortionArdy));

			paymentService.allowPaymentNoPersist(
					FsePaymentRequest.builder().trustlineIssuerClassicAddress(FSE_ISSUING_ADDRESS).currencyName("FSE")
							.amount(String.valueOf(feePortionArdy.stripTrailingZeros()))
							.fromClassicAddress(p.getFromClassicAddress()).fromPrivateKey(p.getFromPrivateKey())
							.fromSigningPublicKey(p.getFromSigningPublicKey())
							.maxXrpFeePerTransaction(p.getMaxXrpFeePerTransaction()).build(),
					SERVICE_FEE_ADDRESS.toBuilder().id(0l).dropRequestId(p.getId()).createDate(now())
							.payAmount(String.valueOf(feePortionArdy.stripTrailingZeros())).updateDate(now()).build());

			paymentService.allowPaymentNoPersist(
					FsePaymentRequest.builder().trustlineIssuerClassicAddress(FSE_ISSUING_ADDRESS).currencyName("FSE")
							.amount(String.valueOf(feePortionBurn.stripTrailingZeros()))
							.fromClassicAddress(p.getFromClassicAddress()).fromPrivateKey(p.getFromPrivateKey())
							.fromSigningPublicKey(p.getFromSigningPublicKey())
							.maxXrpFeePerTransaction(p.getMaxXrpFeePerTransaction()).build(),
					BURN_ISSUER_FSE_ADDRESS.toBuilder().id(0l).dropRequestId(p.getId()).createDate(now())
							.payAmount(String.valueOf(feePortionBurn.stripTrailingZeros())).updateDate(now()).build());

			BigDecimal totalPaid = remainingToCollect.add(p.getFeesPaid() == null ? BigDecimal.ZERO
					: new BigDecimal(p.getFeesPaid()).setScale(2, RoundingMode.DOWN));

			p.setFeesPaid(String.valueOf(totalPaid.doubleValue()));

			return paymentRequestRepo.save(p);
		}

		return p;

	}

	protected boolean isCrossSnapshotDrop(PaymentRequestEnt p) {
		return !StringUtils.isEmpty(p.getSnapshotCurrencyName()) && (!p.getSnapshotCurrencyName()
				.equals(p.getCurrencyName())
				|| (!p.getSnapshotTrustlineIssuerClassicAddress().equals(p.getTrustlineIssuerClassicAddress())));

	}

	private boolean isVerifiedDropType(DropType dropType) {
		return DropType.GLOBALID == dropType || DropType.GLOBALID_SPECIFICADDRESSES == dropType;
	}

	protected BigDecimal getTotalFeeRequiredForSize(int size, DropType dropType, boolean sendingDifferentIssuer) {

		int serviceFeeInterval = isVerifiedDropType(dropType) ? configService.getInt(SERVICE_FEE_INTERVAL_VERIFIED)
				: configService.getInt(SERVICE_FEE_INTERVAL_UNVERIFIED);

		int intervalCount = size / serviceFeeInterval;

		if (size % serviceFeeInterval > 0) {
			intervalCount++;
		}

		double serviceFee = isVerifiedDropType(dropType)
				? configService.getDouble(DEFAULT_SERVICE_FEE_PER_INTERVAL_VERFIED)
				: configService.getDouble(DEFAULT_SERVICE_FEE_PER_INTERVAL_UNVERFIED);

		if (sendingDifferentIssuer) {
			// double fee if sending different issuer
			serviceFee = serviceFee * 2;
			log.info("getTotalFeeRequiredForSide - sendingDifferentIssuer for drop - service Fee - " + serviceFee);
		}

		return new BigDecimal(intervalCount * serviceFee).setScale(2, RoundingMode.DOWN);

	}

	@Override
	public void resetUuidForProcessing(PaymentRequestEnt paymentRequest) {

		if (paymentRequest.getLockUuid() == null) {
			return;
		}
		Optional<PaymentRequestEnt> fromDb = paymentRequestRepo.findById(paymentRequest.getId());
		if (fromDb.isPresent() && DropRequestStatus.REJECTED == fromDb.get().getStatus()) {
			// this drop was cancelled
			return;
		}

		paymentRequestRepo.resetStatusByUuid(paymentRequest.getLockUuid());
		paymentRequestRepo.resetNullUuidByUuid(paymentRequest.getLockUuid());

	}

	protected Date now() {
		return new Date();
	}
}
