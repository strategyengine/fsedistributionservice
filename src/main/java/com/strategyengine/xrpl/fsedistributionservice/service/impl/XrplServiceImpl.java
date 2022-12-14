package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;
import org.xrpl.xrpl4j.model.client.accounts.TrustLine;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.hazelcast.internal.util.StringUtil;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.GlobalIdClient;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientServiceImpl;
import com.strategyengine.xrpl.fsedistributionservice.entity.CancelDropRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropScheduleEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropFrequency;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRecipientStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRequestStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropScheduleStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropType;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.PaymentType;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseSort;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.model.UserAddresses;
import com.strategyengine.xrpl.fsedistributionservice.repo.CancelDropRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropRecipientRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropScheduleRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropSummaryService;
import com.strategyengine.xrpl.fsedistributionservice.service.BlacklistService;
import com.strategyengine.xrpl.fsedistributionservice.service.ConfigService;
import com.strategyengine.xrpl.fsedistributionservice.service.CurrencyHexService;
import com.strategyengine.xrpl.fsedistributionservice.service.EmailService;
import com.strategyengine.xrpl.fsedistributionservice.service.NftService;
import com.strategyengine.xrpl.fsedistributionservice.service.PaymentService;
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
	protected PaymentService paymentService;

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
	protected BlacklistService blacklistService;

	@VisibleForTesting
	@Autowired
	protected GlobalIdClient globalIdClient;

	@VisibleForTesting
	@Autowired
	protected TransactionHistoryService transactionHistoryService;

	@VisibleForTesting
	@Autowired
	protected DropRecipientRepo dropRecipientRepo;

	@VisibleForTesting
	@Autowired
	protected PaymentRequestRepo paymentRequestRepo;

	@VisibleForTesting
	@Autowired
	protected CancelDropRequestRepo cancelDropRequestRepo;

	@Autowired
	protected NftService nftService;

	@Autowired
	protected ConfigService configService;

	@VisibleForTesting
	@Autowired
	protected DropScheduleRepo dropScheduleRepo;

	@Autowired
	protected EmailService emailService;

	private String environment = System.getenv("ENV");

	protected ExecutorService executorPopulateAddresses = Executors.newFixedThreadPool(5);

	@PreDestroy
	public void shutdown() {
		try {
			executorPopulateAddresses.shutdown();
		} catch (Exception e) {
			log.error("Error shutting down executorPopulateAddresses", e);
		}
	}

	@PostConstruct
	public void init() {
		// wipe out any lock uuids for this instance
		List<PaymentRequestEnt> payments = paymentRequestRepo.findAll(Example.of(
				PaymentRequestEnt.builder().status(DropRequestStatus.IN_PROGRESS).environment(environment).build()));
		payments.addAll(paymentRequestRepo.findAll(Example.of(PaymentRequestEnt.builder()
				.status(DropRequestStatus.POPULATING_ADDRESSES).environment(environment).build())));
		payments.addAll(paymentRequestRepo.findAll(Example
				.of(PaymentRequestEnt.builder().status(DropRequestStatus.QUEUED).environment(environment).build())));

		payments.stream().filter(p -> p.getLockUuid() != null)
				.forEach(p -> paymentRequestRepo.save(p.toBuilder().lockUuid(null).build()));

	}

	// @Cacheable("trustlineCache")
	@Override
	public List<FseTrustLine> getTrustLines(String classicAddress, Optional<String> currency,
			Optional<String> currencyForProcess, boolean includes, FseSort sort) {

		return getTrustLinesCompared(classicAddress, currency, currencyForProcess, includes, sort, 0);
	}

	private List<FseTrustLine> getTrustLinesCompared(String classicAddress, Optional<String> currency,
			Optional<String> currencyForProcess, boolean includes, FseSort sort, int attempt) {

		try {
			attempt++;
			return getTrustLinesForCompare(classicAddress, currency, currencyForProcess, includes, sort);

		} catch (Exception e) {
			log.error("Error comparing trustlines " + classicAddress, e);
			if (attempt < 6) {
				return getTrustLinesCompared(classicAddress, currency, currencyForProcess, includes, sort, attempt);
			}
			return null;
		}

	}

	private List<FseTrustLine> getTrustLinesForCompare(String classicAddress, Optional<String> currency,
			Optional<String> currencyForProcess, boolean includes, FseSort sort) {
		try {

			validationService.validateClassicAddress(classicAddress);
			AccountLinesResult trustLines = getTrustLinesWithRetry(classicAddress, 0, null);

			if (trustLines == null) {
				return null;
			}
			List<FseTrustLine> fseTrustLines = trustLines.lines().stream()
					.filter(t -> acceptByCurrency(t, currency, currencyForProcess, includes))
					.map(t -> FseTrustLine.builder().classicAddress(t.account().value()).currency(t.currency())
							.balance(t.balance().replaceFirst("-", "")).limit(t.limitPeer()).build())
					.collect(Collectors.toList());

			if (FseSort.RICH.equals(sort)) {
				fseTrustLines
						.sort((a, b) -> (Double.valueOf(a.getBalance()).compareTo(Double.valueOf(b.getBalance()))));
			} else if (FseSort.OLD.equals(sort)) {
				// put the oldest trustlines first, OG sort
				Collections.reverse(fseTrustLines);
			}
			return fseTrustLines;

		} catch (Exception e) {
			log.error("Error getting trustlines for {} {} {}", classicAddress, currency, currencyForProcess, e);
		}
		return null;
	}

	@Nullable
	private AccountLinesResult getTrustLinesWithRetry(String classicAddress, int attempt,
			AccountLinesResult emptyLines) {
		try {
			attempt++;
			if (attempt > 3) {
				return emptyLines;
			}

			AccountLinesResult tls = xrplClientService.getTrustLines(classicAddress);
			if (tls.lines().isEmpty()) {
				return getTrustLinesWithRetry(classicAddress, attempt, tls);
			}
			log.info("Completed fetching " + tls.lines().size() + " trustlines for " + classicAddress);
			return tls;
		} catch (Exception e) {
			log.error("Error fetching trustlines for " + classicAddress, e);
			return getTrustLinesWithRetry(classicAddress, attempt, emptyLines);
		}

	}

	// @("trustlineSortCache")
	@Override
	public List<FseTrustLine> getTrustLines(String classicAddress, FseSort sort) {
		validationService.validateClassicAddress(classicAddress);
		return getTrustLines(classicAddress, Optional.empty(), Optional.empty(), true, sort);
	}

	private boolean acceptByCurrency(TrustLine t, Optional<String> currency, Optional<String> currencyForProcess,
			boolean includes) {
		if (currency.isEmpty() || currencyForProcess.isEmpty()) {
			return true;
		}
		return includes ? currencyForProcess.get().equals(t.currency()) || currency.get().equals(t.currency())
				: !(currency.get().equals(t.currency()) || currencyForProcess.get().equals(t.currency()));
	}

	@Override
	public List<FseAccount> getAccountInfo(List<String> classicAddresses, boolean includeTrust) {
		return classicAddresses.stream().map(a -> getAccountInfo(a, includeTrust)).collect(Collectors.toList());
	}

	private FseAccount getAccountInfo(String classicAddress) {

		return getAccountInfo(classicAddress, true);
	}

	private FseAccount getAccountInfo(String classicAddress, boolean includeTrust) {
		validationService.validateClassicAddress(classicAddress);
		try {
			FseAccount account = xrplClientService.getAccountInfo(classicAddress);

			return includeTrust ? account.toBuilder().trustLines(getTrustLines(classicAddress, FseSort.OLD)).build()
					: account;
		} catch (Exception e) {
			log.error("Error getting account info", e);
		}
		return null;
	}

	@Override
	public PaymentRequestEnt sendFsePayment(FsePaymentRequest paymentRequestPre) {

		if (!configService.isAirdropEnabled()) {
			throw new BadRequestException("Airdrops down for maintenance.");
		}

		// don't persist this, just kick it back
		try {
			validationService
					.validateAirdropNotAlreadyQueuedForIssuer(paymentRequestPre.getTrustlineIssuerClassicAddress());
			validationService.validateAirdropNotAlreadyQueuedForFromAddress(paymentRequestPre.getFromClassicAddress());
		} catch (BadRequestException e) {
			log.info("Airdrop rejected " + paymentRequestPre + " " + e.getMessage());
			throw e;
		}

		String currency = currencyHexService.fixCurrencyCode(paymentRequestPre.getCurrencyName().trim());
		FsePaymentRequest paymentRequest = paymentRequestPre.toBuilder().currencyName(currency).build();

		if (!paymentRequest.isAgreeFee()) {
			throw new BadRequestException(
					"This transactions requires you to agree to the service fee.  Help keep this service running in the cloud");
		}

		PaymentRequestEnt paymentRequestEnt = paymentRequestRepo.save(PaymentRequestEnt.builder()
				.contactEmail(paymentRequestPre.getEmail()).populateEnvironment(environment)
				.amount(paymentRequest.getAmount().trim()).createDate(now())
				.currencyName(paymentRequestPre.getCurrencyName().trim()).currencyNameForProcess(currency)
				.dropType(paymentRequest.isGlobalIdVerified() ? DropType.GLOBALID_SPECIFICADDRESSES
						: DropType.SPECIFICADDRESSES)
				.startTime(paymentRequestPre.getStartTime() != null ? paymentRequestPre.getStartTime() : new Date())
				.fromClassicAddress(paymentRequest.getFromClassicAddress().trim())
				.paymentType(paymentRequest.getPaymentType())
				.snapshotCurrencyName(paymentRequest.getSnapshotCurrencyName())
				.snapshotTrustlineIssuerClassicAddress(paymentRequest.getSnapshotTrustlineIssuerClassicAddress())
				.fromPrivateKey(paymentRequest.getFromPrivateKey().trim())
				.fromSigningPublicKey(paymentRequest.getFromSigningPublicKey().trim()).maximumTrustlines(null)
				.useBlacklist(paymentRequest.isUseBlacklist()).newTrustlinesOnly(false)
				.status(DropRequestStatus.POPULATING_ADDRESSES)
				.maxXrpFeePerTransaction(paymentRequest.getMaxXrpFeePerTransaction() == null
						? XrplClientServiceImpl.MAX_XRP_FEE_PER_TRANSACTION
						: paymentRequest.getMaxXrpFeePerTransaction())
				.retryOfId(paymentRequest.getRetryOfId())
				.trustlineIssuerClassicAddress(paymentRequest.getTrustlineIssuerClassicAddress().trim())
				.nftIssuerAddress(paymentRequest.getNftIssuingAddress()).nftTaxon(paymentRequest.getNftTaxon())
				.updateDate(now()).build());
		if (blacklistService.getBlackListedCurrencies().contains(paymentRequest.getCurrencyName())) {
			rejectDrop(
					"Airdrops disabled.  The open source tool can be downloaded here https://github.com/strategyengine/fsedistributionservice",
					paymentRequestEnt);
		}

		try {
			validationService.validate(paymentRequest);
		} catch (BadRequestException e) {
			rejectDrop(e, paymentRequestEnt);
		}

		Set<DropRecipientEnt> recipients = null;
		// populate recipients from specific list or from NFT lookup
		if (paymentRequest.getToClassicAddresses() == null && paymentRequest.getNftIssuingAddress() != null) {
			try {
				recipients = nftService.getNftOwners(paymentRequest).stream()
						.filter(n -> !n.getOwner().equals(n.getIssuer()))// don't add a recipient if the issuer owns the
																			// NFT
						.map(t -> DropRecipientEnt.builder().status(DropRecipientStatus.QUEUED).retryAttempt(0)
								.payAmount(paymentRequestEnt.getAmount()).createDate(now()).updateDate(now())
								.dropRequestId(paymentRequestEnt.getId()).address(t.getOwner())
								.ownedNftId(t.getNfTokenID()).build())
						.collect(Collectors.toSet());
			} catch (Exception e) {
				rejectDrop(
						"Failed to populate NFT owners.  You can retry this if the input NFT issuer address is correct: "
								+ paymentRequestEnt.getNftIssuerAddress(),
						paymentRequestEnt);
			}
		} else {

			recipients = paymentRequest.getToClassicAddresses().stream()
					.map(t -> DropRecipientEnt.builder().status(DropRecipientStatus.QUEUED).retryAttempt(0)
							.payAmount(paymentRequestEnt.getAmount()).createDate(now()).updateDate(now())
							.dropRequestId(paymentRequestEnt.getId()).address(t.trim()).build())
					.collect(Collectors.toSet());
		}

		recipients = globalIdFilterSpecificAdds(recipients, paymentRequestPre);

		if (recipients.isEmpty()) {
			rejectDrop("No addresses to pay.  Are you limiting your drop to verified addresses only?",
					paymentRequestEnt);
		}

		FseAccount fromAccount = getAccountInfo(paymentRequestEnt.getFromClassicAddress());

		if (fromAccount == null) {
			fromAccount = getAccountInfo(paymentRequestEnt.getFromClassicAddress());
			if (fromAccount == null) {
				String message = "Could not find valid account for " + paymentRequestEnt.getFromClassicAddress();
				rejectDrop(message, paymentRequestEnt);
			}
		}

		Optional<FseTrustLine> fromAddressTrustLineForFSE = fromAccount.getTrustLines().stream()
				.filter(t -> "FSE".equals(t.getCurrency())
						&& AirdropVerificationServiceImpl.FSE_ISSUING_ADDRESS.equals(t.getClassicAddress()))
				.findFirst();

		if (fromAddressTrustLineForFSE.isEmpty()) {
			String message = String.format(
					"The from address must have a trustline set for FSE and some FSE to cover the airdrop FEE. FSE issuing address: %s currency: FSE.  "
							+ "You can also add the trustline using xumm here https://xumm.community/?issuer=rs1MKY54miDtMFEGyNuPd3BLsXauFZUSrj&currency=FSE&limit=100000",
					AirdropVerificationServiceImpl.FSE_ISSUING_ADDRESS);
			rejectDrop(message, paymentRequestEnt);
		}

		try {
			validationService.validateFseBalance(Double.parseDouble(fromAddressTrustLineForFSE.get().getBalance()),
					recipients.size(), paymentRequestEnt.getDropType(), false);
			// validationService.validateDistributingTokenBalance(fromAddressTrustLine,
			// paymentRequestEnt.getAmount(), recipients.size());

		} catch (BadRequestException e) {
			rejectDrop(e, paymentRequestEnt);
		}

		List<DropRecipientEnt> savedRecipients = dropRecipientRepo.saveAll(removeDuplicates(recipients));

		if (paymentRequestPre.getToClassicAddresses() != null && !paymentRequestPre.getToClassicAddresses().isEmpty()
				&& paymentRequestPre.getNftIssuingAddress() == null) {
			logRecipientCountDiff(paymentRequestPre, recipients, savedRecipients, paymentRequestEnt.getId());
		}

		if (cancelDropRequestRepo
				.exists(Example.of(CancelDropRequestEnt.builder().dropRequestId(paymentRequestEnt.getId()).build()))) {
			try {
				paymentRequestEnt.setFailReason("Job Cancelled");
				paymentRequestEnt.setStatus(DropRequestStatus.REJECTED);
				paymentRequestEnt.setFromPrivateKey(null);
				paymentRequestEnt.setFromSigningPublicKey(null);
				return paymentRequestRepo.save(paymentRequestEnt);

			} catch (Exception e) {
				throw new RuntimeException("Canceled job while populating addresses" + paymentRequestEnt, e);
			}
		}

		final PaymentRequestEnt savedPaymentRequest;

		if (PaymentType.PROPORTIONAL.equals(paymentRequestEnt.getPaymentType())) {
			savedPaymentRequest = paymentRequestRepo
					.save(paymentRequestEnt.toBuilder().status(DropRequestStatus.POPULATING_ADDRESSES).build());
			executorPopulateAddresses.execute(() -> updateRecipientAmountThread(savedPaymentRequest, savedRecipients));
		} else {
			savedPaymentRequest = paymentRequestRepo
					.save(paymentRequestEnt.toBuilder().status(DropRequestStatus.PENDING_REVIEW).build());
		}

		return saveSchedule(savedPaymentRequest, paymentRequestPre.getFrequency(), paymentRequestPre.getRepeatUntilDate());

	}


	private void logRecipientCountDiff(FsePaymentRequest paymentRequestPre, Set<DropRecipientEnt> recipients,
			List<DropRecipientEnt> savedRecipients, Long dropRequestId) {

		if (paymentRequestPre.getToClassicAddresses().size() > recipients.size()) {
			List<String> recipAddList = recipients.stream().map(r -> r.getAddress()).collect(Collectors.toList());
			paymentRequestPre.getToClassicAddresses().stream().filter(r -> !recipAddList.contains(r))
					.forEach(r -> log.info(String.format(
							"recipients check - recipient %s removed from drop id %s.  Could be blacklisted or globalid",
							r, dropRequestId)));

		} else if (paymentRequestPre.getToClassicAddresses().size() > savedRecipients.size()) {
			List<String> recipAddList = recipients.stream().map(r -> r.getAddress()).collect(Collectors.toList());
			paymentRequestPre.getToClassicAddresses().stream().filter(r -> !recipAddList.contains(r))
					.forEach(r -> log.warn(String.format(
							"savedRecipients check - recipient %s removed from drop id %s.  Should not happen!", r,
							dropRequestId)));
		}

	}

	private void updateRecipientAmountThread(PaymentRequestEnt paymentReq, List<DropRecipientEnt> savedRecipients) {

		String issuingAddressForProportion = StringUtils
				.hasLength(paymentReq.getSnapshotTrustlineIssuerClassicAddress())
						? paymentReq.getSnapshotTrustlineIssuerClassicAddress()
						: paymentReq.getTrustlineIssuerClassicAddress();

		String currencyForProportion = StringUtils.hasLength(paymentReq.getSnapshotCurrencyName())
				? paymentReq.getSnapshotCurrencyName()
				: paymentReq.getCurrencyName();

		final List<FseTrustLine> trustlines;
		if (savedRecipients.size() > 20) {
			trustlines = getTrustLines(issuingAddressForProportion, Optional.ofNullable(currencyForProportion),
					Optional.empty(), true, FseSort.RICH);
		} else {
			trustlines = ImmutableList.of();
		}
		savedRecipients.stream().forEach(r -> updatePaymentAmount(r, trustlines, paymentReq,
				issuingAddressForProportion, currencyForProportion));

		if (paymentReq.getStartTime() != null) {
			paymentReq.setStatus(DropRequestStatus.SCHEDULED);
		} else {
			paymentReq.setStatus(DropRequestStatus.PENDING_REVIEW);
		}
		paymentRequestRepo.save(paymentReq);

	}

	private void updatePaymentAmount(DropRecipientEnt recip, List<FseTrustLine> trustlines,
			PaymentRequestEnt paymentReq, String issuingAddressForProportion, String currencyNameForProportion) {
		String currencyNameCorrected = currencyHexService.fixCurrencyCode(currencyNameForProportion);
		List<FseTrustLine> trustLinesMatchedForIssuer = trustlines.stream()
				.filter(t -> t.getClassicAddress().equals(recip.getAddress())).collect(Collectors.toList());

		Optional<FseTrustLine> trustLineOp = Optional.empty();
		if (!trustLinesMatchedForIssuer.isEmpty()) {
			if (trustLinesMatchedForIssuer.size() == 1) {
				trustLineOp = Optional.of(trustLinesMatchedForIssuer.get(0));
			} else {
				trustLineOp = trustLinesMatchedForIssuer.stream()
						.filter(t -> t.getCurrency().equals(currencyNameCorrected)).findAny();
			}
		}
		// TODO validate these trustline addresses are recipient addresses
		String balance = null;
		if (trustLineOp.isEmpty()) {
			// get trustlines of this specific account
			List<FseTrustLine> trustLinesOfRecipient = this.getTrustLines(recip.getAddress(), FseSort.RICH);

			final Optional<FseTrustLine> recipTrustLineOp;
			if (trustLinesOfRecipient == null) {
				recipTrustLineOp = Optional.empty();
			} else {
				recipTrustLineOp = trustLinesOfRecipient.stream()
						.filter(t -> t.getClassicAddress().equals(issuingAddressForProportion)
								&& t.getCurrency().equals(currencyNameCorrected))
						.findAny();
			}
			balance = recipTrustLineOp.isEmpty() ? null : recipTrustLineOp.get().getBalance();
		} else {
			balance = trustLineOp.get().getBalance();
		}

		if (balance == null) {
			// No trustline found for recipient
			recip.setStatus(DropRecipientStatus.FAILED);
			recip.setFailReason("No trustline found");
			dropRecipientRepo.save(recip);
			return;
		}

		String payAmount = calculateRecipientPayAmount(balance, paymentReq.getAmount(), paymentReq.getPaymentType());
		recip.setPayAmount(payAmount);
		recip.setSnapshotBalance(balance);
		dropRecipientRepo.save(recip);

	}

	private void rejectDrop(String message, PaymentRequestEnt paymentRequestEnt) {
		paymentRequestEnt.setFailReason(message);
		paymentRequestEnt.setStatus(DropRequestStatus.REJECTED);
		String m = message != null && message.length() > 200 ? message.substring(0, 200) : message;
		log.info("Rejected drop " + paymentRequestEnt, m);
		paymentRequestEnt.setFromPrivateKey(null);
		paymentRequestEnt.setFromSigningPublicKey(null);
		paymentRequestRepo.save(paymentRequestEnt);
		throw new BadRequestException(message);

	}

	private void rejectDrop(BadRequestException e, PaymentRequestEnt paymentRequestEnt) {
		paymentRequestEnt.setFailReason(e.getMessage());
		paymentRequestEnt.setStatus(DropRequestStatus.REJECTED);
		String m = e.getMessage() != null && e.getMessage().length() > 200 ? e.getMessage().substring(0, 200)
				: e.getMessage();
		log.info("Rejected drop " + paymentRequestEnt, m);
		paymentRequestEnt.setFromPrivateKey(null);
		paymentRequestEnt.setFromSigningPublicKey(null);
		paymentRequestRepo.save(paymentRequestEnt);
		throw e;

	}

	@Override
	public PaymentRequestEnt sendFsePaymentToTrustlines(@NonNull FsePaymentTrustlinesRequest paymentRequestPre,
			@Nullable List<DropRecipientEnt> retryFailedAddresses) {

		if (!configService.isAirdropEnabled()) {
			throw new BadRequestException("Airdrops down for maintenance.");
		}

		try {
			validationService
					.validateAirdropNotAlreadyQueuedForIssuer(paymentRequestPre.getTrustlineIssuerClassicAddress());
			validationService.validateAirdropNotAlreadyQueuedForFromAddress(paymentRequestPre.getFromClassicAddress());
		} catch (BadRequestException e) {
			log.info("Airdrop rejected " + paymentRequestPre + " " + e.getMessage());
			throw e;
		}

		String currencyNameForProcess = currencyHexService.fixCurrencyCode(paymentRequestPre.getCurrencyName().trim());
		FsePaymentTrustlinesRequest p = paymentRequestPre.toBuilder().currencyName(currencyNameForProcess).build();

		final PaymentRequestEnt paymentRequestEnt = paymentRequestRepo.save(PaymentRequestEnt.builder().minBalance(
				paymentRequestPre.getMinBalance() != null ? String.valueOf(paymentRequestPre.getMinBalance()) : null)
				.contactEmail(paymentRequestPre.getEmail())
				.startTime(paymentRequestPre.getStartTime() != null ? paymentRequestPre.getStartTime() : new Date())
				.maxBalance(
						paymentRequestPre.getMaxBalance() != null ? String.valueOf(paymentRequestPre.getMaxBalance())
								: null)
				.paymentType(paymentRequestPre.getPaymentType() == null ? PaymentType.FLAT
						: paymentRequestPre.getPaymentType())
				.populateEnvironment(environment).amount(p.getAmount().trim()).createDate(now())
				.currencyName(paymentRequestPre.getCurrencyName().trim())
				.currencyNameForProcess(p.getCurrencyName().trim())
				.maxXrpFeePerTransaction(paymentRequestPre.getMaxXrpFeePerTransaction() == null
						? XrplClientServiceImpl.MAX_XRP_FEE_PER_TRANSACTION
						: paymentRequestPre.getMaxXrpFeePerTransaction())
				.retryOfId(paymentRequestPre.getRetryOfId())
				.dropType(paymentRequestPre.isGlobalIdVerified() ? DropType.GLOBALID : DropType.TRUSTLINE)
				.fromClassicAddress(p.getFromClassicAddress().trim()).fromPrivateKey(p.getFromPrivateKey().trim())
				.fromSigningPublicKey(p.getFromSigningPublicKey().trim()).maximumTrustlines(p.getMaximumTrustlines())
				.newTrustlinesOnly(p.isNewTrustlinesOnly()).status(DropRequestStatus.POPULATING_ADDRESSES)
				.useBlacklist(p.isUseBlacklist()).snapshotCurrencyName(p.getSnapshotCurrencyName())
				.snapshotTrustlineIssuerClassicAddress(p.getSnapshotTrustlineIssuerClassicAddress())
				.trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress().trim()).updateDate(now()).build());

		PaymentRequestEnt savedPayment = saveSchedule(paymentRequestEnt, paymentRequestPre.getFrequency(),
				paymentRequestPre.getRepeatUntilDate());
				
		executorPopulateAddresses.execute(
				() -> sendFsePaymentToTrustlinesThread(savedPayment, retryFailedAddresses, p, paymentRequestPre));

		return savedPayment;

	}

	private PaymentRequestEnt saveSchedule(PaymentRequestEnt paymentRequestEnt, DropFrequency frequency, Date repeatUntilDate) {

		if (paymentRequestEnt.getStartTime() == null || frequency == null) {
			return paymentRequestEnt;
		}

		dropScheduleRepo
				.save(DropScheduleEnt.builder().createDate(new Date()).dropRequestId(paymentRequestEnt.getId())
						.dropScheduleStatus(DropScheduleStatus.ACTIVE).frequency(frequency)
						.repeatUntilDate(repeatUntilDate).build());

		emailService.sendEmail(paymentRequestEnt.getContactEmail(), "Airdrop Scheduled",
				"Airdrop has been scheduled.  <a href='https://strategyengine.one/#/airdropdetails?dropRequestId="
						+ paymentRequestEnt.getId() + "'>Schedule Details</a>");

		return paymentRequestRepo.save(paymentRequestEnt.toBuilder().status(DropRequestStatus.SCHEDULED).build());

	}

	private PaymentRequestEnt sendFsePaymentToTrustlinesThread(PaymentRequestEnt paymentRequestEnt,
			@Nullable List<DropRecipientEnt> retryFailedAddresses, FsePaymentTrustlinesRequest p,
			@NonNull FsePaymentTrustlinesRequest paymentRequestPre) {

		if (blacklistService.getBlackListedCurrencies().contains(p.getCurrencyName())) {
			rejectDrop(
					"Airdrops disabled. The open source tool can be downloaded here https://github.com/strategyengine/fsedistributionservice",
					paymentRequestEnt);
		}

		if (!p.isAgreeFee()) {
			rejectDrop("This transaction requires you to agree to the service fee.", paymentRequestEnt);
		}

		final Set<String> previouslyPaidAddresses;

		if (p.isNewTrustlinesOnly() && retryFailedAddresses == null && !"XRP".equals(p.getCurrencyName())) {
			previouslyPaidAddresses = transactionHistoryService.getPreviouslyPaidAddresses(p.getFromClassicAddress(),
					p.getTrustlineIssuerClassicAddress());
		} else {
			previouslyPaidAddresses = new HashSet<String>();
		}

		try {
			validationService.validate(p);
		} catch (BadRequestException e) {
			rejectDrop(e, paymentRequestEnt);
		}

		FseAccount fromAccount = getAccountInfo(p.getFromClassicAddress());

		if (fromAccount == null) {
			fromAccount = getAccountInfo(p.getFromClassicAddress());
			if (fromAccount == null) {
				String message = "Could not find valid account for " + p.getFromClassicAddress();
				rejectDrop(message, paymentRequestEnt);
			}
		}

		Optional<FseTrustLine> fromAddressTrustLine = fromAccount.getTrustLines().stream()
				.filter(t -> p.getTrustlineIssuerClassicAddress().equals(t.getClassicAddress())
						&& (p.getCurrencyName().equals(t.getCurrency())
								|| (currencyHexService.fixCurrencyCode(p.getCurrencyName()).equals(t.getCurrency()))))
				.findFirst();

		Optional<FseTrustLine> fromAddressTrustLineForFSE = fromAccount.getTrustLines().stream()
				.filter(t -> "FSE".equals(t.getCurrency())
						&& AirdropVerificationServiceImpl.FSE_ISSUING_ADDRESS.equals(t.getClassicAddress()))
				.findFirst();

		List<FseTrustLine> eligibleTrustLines;
		if (retryFailedAddresses == null) {
			List<FseTrustLine> trustLines = fetchAllTrustlines(paymentRequestEnt);

			eligibleTrustLines = fetchEligibleTrustlines(trustLines, paymentRequestEnt, paymentRequestPre, p,
					previouslyPaidAddresses);

		} else {
			eligibleTrustLines = new ArrayList<>();
		}

		if (fromAddressTrustLineForFSE.isEmpty()) {
			String message = String.format(
					"The from address must have a trustline set for FSE and some FSE to cover the airdrop FEE. FSE issuing address: %s currency: FSE.  You can also add the trustline using xumm here https://xumm.community/?issuer=rs1MKY54miDtMFEGyNuPd3BLsXauFZUSrj&currency=FSE&limit=100000",
					AirdropVerificationServiceImpl.FSE_ISSUING_ADDRESS);
			rejectDrop(message, paymentRequestEnt);
		}

		if (eligibleTrustLines.isEmpty() && retryFailedAddresses == null) {
			String message = String.format(
					"No eligible addresses found for issuingAddress %s currency %s newOnly:%s maxTrustLines:%s",
					p.getTrustlineIssuerClassicAddress(), p.getCurrencyName(), p.isNewTrustlinesOnly(),
					p.getMaximumTrustlines());
			rejectDrop(message, paymentRequestEnt);
		}

		try {
			validationService.validateFseBalance(Double.parseDouble(fromAddressTrustLineForFSE.get().getBalance()),
					retryFailedAddresses == null ? eligibleTrustLines.size() : retryFailedAddresses.size(),
					paymentRequestEnt.getDropType(), isCrossCurrencyDrop(paymentRequestEnt));
			if (retryFailedAddresses == null) {
				// will not be present for retry
				if ("XRP".equals(paymentRequestEnt.getCurrencyName())) {
					fromAddressTrustLine = Optional
							.of(FseTrustLine.builder().balance(String.valueOf(fromAccount.getXrpBalance())).build());
				}
				validationService.validateDistributingTokenBalance(fromAddressTrustLine, p.getAmount(),
						eligibleTrustLines.size());
			}
		} catch (BadRequestException e) {
			rejectDrop(e, paymentRequestEnt);
		}

		log.info("Found eligible TrustLines to send to.  Size: {}",
				retryFailedAddresses == null ? eligibleTrustLines.size() : retryFailedAddresses.size());

		Date now = now();
		Set<DropRecipientEnt> recipients;
		if (retryFailedAddresses == null) {
			recipients = eligibleTrustLines.stream()
					.map(t -> DropRecipientEnt.builder().status(DropRecipientStatus.QUEUED).retryAttempt(0)
							.createDate(now).updateDate(now).dropRequestId(paymentRequestEnt.getId())
							.snapshotBalance(t.getBalance())
							.payAmount(calculateRecipientPayAmount(t.getBalance(), paymentRequestEnt.getAmount(),
									paymentRequestEnt.getPaymentType()))
							.address(t.getClassicAddress()).build())
					.collect(Collectors.toSet());
		} else {

			recipients = retryFailedAddresses.stream()
					.map(r -> r.toBuilder().retryAttempt(r.getRetryAttempt() + 1).createDate(now()).updateDate(now())
							.status(DropRecipientStatus.QUEUED).id(null).dropRequestId(paymentRequestEnt.getId())
							.build())
					.collect(Collectors.toSet());
		}

		dropRecipientRepo.saveAll(removeDuplicates(recipients));

		if (cancelDropRequestRepo
				.exists(Example.of(CancelDropRequestEnt.builder().dropRequestId(paymentRequestEnt.getId()).build()))) {
			try {
				paymentRequestEnt.setFailReason("Job Cancelled");
				paymentRequestEnt.setStatus(DropRequestStatus.REJECTED);
				paymentRequestEnt.setFromPrivateKey(null);
				paymentRequestEnt.setFromSigningPublicKey(null);
				return paymentRequestRepo.save(paymentRequestEnt);

			} catch (Exception e) {
				throw new RuntimeException("Canceled job while populating addresses" + paymentRequestEnt, e);
			}
		}

		DropRequestStatus status = paymentRequestEnt.getStartTime() != null ? DropRequestStatus.PENDING_REVIEW
				: DropRequestStatus.SCHEDULED;
		return paymentRequestRepo.save(paymentRequestEnt.toBuilder().status(status).build());

	}

	private List<FseTrustLine> fetchEligibleTrustlines(List<FseTrustLine> trustLines,
			PaymentRequestEnt paymentRequestEnt, @NonNull FsePaymentTrustlinesRequest paymentRequestPre,
			FsePaymentTrustlinesRequest p, Set<String> previouslyPaidAddresses) {

		// filter out trustlines that are not elibigle for the final payment list
		List<FseTrustLine> eligibleTrustLines = trustLines.stream()
				.filter(t -> p.isNewTrustlinesOnly() ? !previouslyPaidAddresses.contains(t.getClassicAddress()) : true)
				.filter(t -> matchesMinMax(p, t)).collect(Collectors.toList());

		eligibleTrustLines = globalIdFilterTrustlines(eligibleTrustLines, paymentRequestPre);

		if (p.getMaximumTrustlines() != null && eligibleTrustLines.size() > p.getMaximumTrustlines()
				&& p.getMaximumTrustlines() < eligibleTrustLines.size()) {
			// Some airdroppers only want to pay the first X number fo trustlines created.
			// others want to pay a max number of trustlines wherever created. Needs to be
			// broken out to 2 variables
			eligibleTrustLines = eligibleTrustLines.subList(0, p.getMaximumTrustlines());
		}

		if (eligibleTrustLines.isEmpty()) {
			String message = String.format("No eligible addresses found for issuingAddress %s currency %s newOnly:%s",
					p.getTrustlineIssuerClassicAddress(), p.getCurrencyName(), p.isNewTrustlinesOnly());
			rejectDrop(message, paymentRequestEnt);
		}
		return eligibleTrustLines;
	}

	@Override
	public List<FseTrustLine> fetchAllTrustlines(FsePaymentTrustlinesRequest paymentRequestPre) {

		String currencyNameForProcess = currencyHexService.fixCurrencyCode(paymentRequestPre.getCurrencyName().trim());
		FsePaymentTrustlinesRequest p = paymentRequestPre.toBuilder().currencyName(currencyNameForProcess).build();

		final PaymentRequestEnt paymentRequestEnt = paymentRequestRepo.save(PaymentRequestEnt.builder().minBalance(
				paymentRequestPre.getMinBalance() != null ? String.valueOf(paymentRequestPre.getMinBalance()) : null)
				.contactEmail(paymentRequestPre.getEmail())
				.maxBalance(
						paymentRequestPre.getMaxBalance() != null ? String.valueOf(paymentRequestPre.getMaxBalance())
								: null)
				.startTime(paymentRequestPre.getStartTime() != null ? paymentRequestPre.getStartTime() : new Date())
				.paymentType(paymentRequestPre.getPaymentType() == null ? PaymentType.FLAT
						: paymentRequestPre.getPaymentType())
				.populateEnvironment(environment).amount(p.getAmount().trim()).createDate(now())
				.currencyName(paymentRequestPre.getCurrencyName().trim())
				.currencyNameForProcess(p.getCurrencyName().trim())
				.maxXrpFeePerTransaction(paymentRequestPre.getMaxXrpFeePerTransaction() == null
						? XrplClientServiceImpl.MAX_XRP_FEE_PER_TRANSACTION
						: paymentRequestPre.getMaxXrpFeePerTransaction())
				.retryOfId(paymentRequestPre.getRetryOfId())
				.dropType(paymentRequestPre.isGlobalIdVerified() ? DropType.GLOBALID : DropType.TRUSTLINE)
				.fromClassicAddress(p.getFromClassicAddress().trim()).fromPrivateKey(p.getFromPrivateKey().trim())
				.fromSigningPublicKey(p.getFromSigningPublicKey().trim()).maximumTrustlines(p.getMaximumTrustlines())
				.newTrustlinesOnly(p.isNewTrustlinesOnly()).status(DropRequestStatus.POPULATING_ADDRESSES)
				.useBlacklist(p.isUseBlacklist()).snapshotCurrencyName(p.getSnapshotCurrencyName())
				.snapshotTrustlineIssuerClassicAddress(p.getSnapshotTrustlineIssuerClassicAddress())
				.trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress().trim()).updateDate(now()).build());

		return fetchAllTrustlines(paymentRequestEnt);
	}

	private List<FseTrustLine> fetchAllTrustlines(PaymentRequestEnt paymentRequestEnt) {
		String snapshotCurrencyName = StringUtils.hasLength(paymentRequestEnt.getSnapshotCurrencyName())
				? paymentRequestEnt.getSnapshotCurrencyName()
				: paymentRequestEnt.getCurrencyName();
		String snapshotIssuerAddress = StringUtils
				.hasLength(paymentRequestEnt.getSnapshotTrustlineIssuerClassicAddress())
						? paymentRequestEnt.getSnapshotTrustlineIssuerClassicAddress()
						: paymentRequestEnt.getTrustlineIssuerClassicAddress();
		String currencyNameForProcess = currencyHexService.fixCurrencyCode(snapshotCurrencyName.trim());

		List<FseTrustLine> trustLines = getTrustLines(snapshotIssuerAddress, Optional.of(snapshotCurrencyName),
				Optional.of(currencyNameForProcess), true, FseSort.OLD);

		if (trustLines == null) {
			String message = String.format(
					"Error trying to fetch trustlines for issuing address %s.  You can retry this request if you think the issuing address is correct.",
					snapshotIssuerAddress);
			rejectDrop(message, paymentRequestEnt);
		}

		if (isCrossCurrencyDrop(paymentRequestEnt)) {

			if ("XRP".equals(paymentRequestEnt.getCurrencyName())) {
				// everybody can receive XRP
				return trustLines;
			}
			// since they are sending to a different issuer address, fetch the trustlines
			// for the sending issuer to make sure each address can receive the payment
			List<FseTrustLine> paymentTrustLines = getTrustLines(paymentRequestEnt.getTrustlineIssuerClassicAddress(),
					Optional.of(paymentRequestEnt.getCurrencyName()),
					Optional.of(currencyHexService.fixCurrencyCode(paymentRequestEnt.getCurrencyName().trim())), true,
					FseSort.OLD);

			if (paymentTrustLines == null) {
				String message = String.format(
						"Error trying to fetch payment trustlines for issuing address %s.  You can retry this request if you think the issuing address is correct.",
						paymentRequestEnt.getTrustlineIssuerClassicAddress());
				rejectDrop(message, paymentRequestEnt);
			}

			Set<String> paymentTrustLinesSet = paymentTrustLines.stream().map(t -> t.getClassicAddress())
					.collect(Collectors.toSet());

			return trustLines.stream().filter(t -> paymentTrustLinesSet.contains(t.getClassicAddress()))
					.collect(Collectors.toList());

		}

		return trustLines;
	}

	private boolean isCrossCurrencyDrop(PaymentRequestEnt paymentRequestEnt) {
		return StringUtils.hasLength(paymentRequestEnt.getSnapshotCurrencyName())
				&& (!paymentRequestEnt.getSnapshotCurrencyName().equals(paymentRequestEnt.getCurrencyName())
						|| (!paymentRequestEnt.getSnapshotTrustlineIssuerClassicAddress()
								.equals(paymentRequestEnt.getTrustlineIssuerClassicAddress())));
	}

	private Set<DropRecipientEnt> removeDuplicates(Set<DropRecipientEnt> recipients) {

		Set<DropRecipientEnt> recipientsToSave = new HashSet<>();
		Set<String> foundRecipients = new HashSet<>();
		for (DropRecipientEnt recip : recipients) {
			if (!StringUtil.isNullOrEmpty(recip.getOwnedNftId())) {
				// NFT owners could receive multiple payments
				return recipients;
			}
			if (!foundRecipients.contains(recip.getAddress())) {
				recipientsToSave.add(recip);
				foundRecipients.add(recip.getAddress());
			}
		}
		return recipientsToSave;
	}

	protected String calculateRecipientPayAmount(String balance, String paymentRequestAmount, PaymentType paymentType) {
		if (PaymentType.FLAT == paymentType) {
			return paymentRequestAmount;
		}

		if (PaymentType.PROPORTIONAL == paymentType) {
			BigDecimal paymentAmount = new BigDecimal(balance).multiply(new BigDecimal(paymentRequestAmount));
			int scale = 4;
			if (paymentAmount.compareTo(new BigDecimal("9999999999")) > 0) {
				scale = 1;
			}
			return String.valueOf(paymentAmount.setScale(scale, RoundingMode.DOWN));
		}

		throw new RuntimeException("PaymentType invalid." + paymentType);
	}

	protected Set<DropRecipientEnt> globalIdFilterSpecificAdds(Set<DropRecipientEnt> recipients, FsePaymentRequest p) {
		if (p.isGlobalIdVerified()) {

			final List<UserAddresses> globalIdXrpAddresses = globalIdClient.getGlobalIdXrpAddresses();

			Set<String> uniqueRecipientAddresses = recipients.stream().map(r -> r.getAddress())
					.collect(Collectors.toSet());
			// this will only pay one address for any given user. A user with multiple XRP
			// addresses will only have one valid address returned
			Set<String> uniqueXrpGidAdds = globalIdXrpAddresses.stream()
					.map(g -> findOneAddressMatch(uniqueRecipientAddresses, g)).filter(g -> g != null)
					.collect(Collectors.toSet());

			return recipients.stream().filter(r -> uniqueXrpGidAdds.contains(r.getAddress()))
					.collect(Collectors.toSet());
		}

		return recipients;
	}

	protected List<FseTrustLine> globalIdFilterTrustlines(final List<FseTrustLine> eligibleTrustLines,
			@NonNull FsePaymentTrustlinesRequest paymentRequestPre) {

		if (paymentRequestPre.isGlobalIdVerified()) {
			final List<UserAddresses> globalIdXrpAddresses = globalIdClient.getGlobalIdXrpAddresses();

			Set<String> uniqueEligibleAddresses = eligibleTrustLines.stream().map(t -> t.getClassicAddress())
					.collect(Collectors.toSet());

			Set<String> uniqueXrpGidAdds = globalIdXrpAddresses.stream()
					.map(g -> findOneAddressMatch(uniqueEligibleAddresses, g)).filter(g -> g != null)
					.collect(Collectors.toSet());

			return eligibleTrustLines.stream().filter(r -> uniqueXrpGidAdds.contains(r.getClassicAddress()))
					.collect(Collectors.toList());
		}
		return eligibleTrustLines;

	}

	private String findOneAddressMatch(Set<String> uniqueEligibleAddresses, UserAddresses uads) {
		for (String uadAddress : uads.getAddresses()) {
			if (uniqueEligibleAddresses.contains(uadAddress)) {
				return uadAddress;
			}
		}
		return null;
	}

	protected boolean matchesMinMax(FsePaymentTrustlinesRequest p, FseTrustLine trustLine) {

		if (p.getMaxBalance() != null && new BigDecimal(trustLine.getBalance()).stripTrailingZeros()
				.compareTo(BigDecimal.valueOf(p.getMaxBalance()).stripTrailingZeros()) > 0) {

			return false;
		}

		if (p.getMinBalance() != null && new BigDecimal(trustLine.getBalance()).stripTrailingZeros()
				.compareTo(BigDecimal.valueOf(p.getMinBalance()).stripTrailingZeros()) < 0) {

			return false;
		}

		return true;
	}

	private PaymentRequestEnt cancelJob(PaymentRequestEnt p) {

		log.info("Cancel requested for job " + p);

		cancelDropRequestRepo.save(CancelDropRequestEnt.builder().dropRequestId(p.getId()).createDate(now()).build());

		if (DropRequestStatus.POPULATING_ADDRESSES.equals(p.getStatus())
				|| DropRequestStatus.PENDING_REVIEW.equals(p.getStatus())
				|| DropRequestStatus.QUEUED.equals(p.getStatus())) {
			paymentRequestRepo
					.save(p.toBuilder().status(DropRequestStatus.REJECTED).failReason("Canceled by user").build());
		}

		return p;

	}

	@Override
	public FsePaymentRequest cancelJob(String privateKey, String issuingAddress) {

		List<PaymentRequestEnt> paymentEnts;
		if (!StringUtils.hasLength(issuingAddress)) {
			paymentEnts = paymentRequestRepo.findActive().stream().filter(p -> "XRP".equals(p.getCurrencyName()))
					.collect(Collectors.toList());
		} else {
			paymentEnts = paymentRequestRepo.findActive().stream()
					.filter(p -> issuingAddress.equals(p.getTrustlineIssuerClassicAddress()))
					.collect(Collectors.toList());
		}

		List<PaymentRequestEnt> cancelablePayments = paymentEnts.stream()
				.filter(p -> p.getStatus() == DropRequestStatus.PENDING_REVIEW
						|| p.getStatus() == DropRequestStatus.IN_PROGRESS || p.getStatus() == DropRequestStatus.QUEUED
						|| p.getStatus() == DropRequestStatus.POPULATING_ADDRESSES)
				.filter(p -> p.getFromPrivateKey().equals(privateKey)).collect(Collectors.toList());

		if (!cancelablePayments.isEmpty()) {

			List<PaymentRequestEnt> cancelled = cancelablePayments.stream().map(p -> cancelJob(p))
					.collect(Collectors.toList());

			PaymentRequestEnt p = cancelled.get(0);

			return FsePaymentRequest.builder().currencyName(p.getCurrencyName()).amount(p.getAmount())
					.fromClassicAddress(p.getFromClassicAddress()).fromSigningPublicKey("BLANK").fromPrivateKey("BLANK")
					.trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress()).build();

		}

		throw new BadRequestException(
				"Could not find active Airdrop for the input private key and issuing address - " + issuingAddress);
	}

	protected Date now() {
		return new Date();
	}

	@Override
	public void approveAirdrop(Long paymentRequestId, String privKey) {
		Optional<PaymentRequestEnt> payReqEnt = paymentRequestRepo.findById(paymentRequestId);
		if (payReqEnt.isEmpty()) {
			throw new BadRequestException("Invalid payment request id");
		}
		if (payReqEnt.get().getFromPrivateKey() == null) {
			payReqEnt.get().setStatus(DropRequestStatus.REJECTED);
			paymentRequestRepo.save(payReqEnt.get());
			throw new BadRequestException("This airdrop has expired.  Please create a new one.");
		}
		if (payReqEnt.get().getFromPrivateKey().equals(privKey)) {
			payReqEnt.get().setStatus(DropRequestStatus.QUEUED);
			paymentRequestRepo.save(payReqEnt.get());
			return;
		}
		throw new BadRequestException("Invalid private key entered");
	}
}
