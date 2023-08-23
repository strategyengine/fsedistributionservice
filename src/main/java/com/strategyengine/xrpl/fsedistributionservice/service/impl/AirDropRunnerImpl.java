package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.entity.CancelDropRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRecipientStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRequestStatus;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.repo.CancelDropRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropRecipientRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropVerificationService;
import com.strategyengine.xrpl.fsedistributionservice.service.ConfigService;
import com.strategyengine.xrpl.fsedistributionservice.service.PaymentService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class AirDropRunnerImpl {

	@VisibleForTesting
	@Autowired
	protected DropRecipientRepo dropRecipientRepo;

	@VisibleForTesting
	@Autowired
	protected PaymentRequestRepo paymentRequestRepo;

	@VisibleForTesting
	@Autowired
	protected CancelDropRequestRepo cancelDropRequestRepo;

	@VisibleForTesting
	@Autowired
	protected PaymentService paymentService;

	@VisibleForTesting
	@Autowired
	protected ConfigService configService;

	@VisibleForTesting
	@Autowired
	protected AirdropVerificationService airdropVerificationService;

	@VisibleForTesting
	@Autowired
	protected TransactionHistoryService transactionHistoryService;

	private String environment = System.getenv("ENV");
	
	public static String ADMIN_LOCK_UUID = "RUN_ADMIN";

	public static final String REASON_CANCEL_BY_USER = "Job Cancelled by user";
	
	// DO NOT MARK THIS METHOD TRANSACTIONAL we need other processes to see the
	// lock_uuid
	// fixedDelay millis - check every minute for a drop
	@Scheduled(fixedDelay = 60000)
	public void runAirDrop() {

		Optional<PaymentRequestEnt> adminPaymentRequestOpt = Optional.empty();
		if ("BEAST1".equals(environment) || "local".equals(environment) || !configService.isAirdropEnabled()) {
			if("local".equals(environment)) {
				adminPaymentRequestOpt = paymentRequestRepo.findOne(
					Example.of(PaymentRequestEnt.builder().lockUuid(ADMIN_LOCK_UUID).build()));
			}
			
			if(adminPaymentRequestOpt.isEmpty() || DropRequestStatus.PENDING_REVIEW.equals(adminPaymentRequestOpt.get().getStatus())
					|| DropRequestStatus.POPULATING_ADDRESSES.equals(adminPaymentRequestOpt.get().getStatus())
					|| DropRequestStatus.SCHEDULED.equals(adminPaymentRequestOpt.get().getStatus())) {
				return;
			}
		}

		PaymentRequestEnt paymentRequest = null;
		String uuid = null;
		if(adminPaymentRequestOpt.isEmpty()) {
			uuid = airdropVerificationService.lockPaymentRequest();
			paymentRequest = airdropVerificationService.getPaymentRequestToProcess(uuid);
			if (paymentRequest == null) {
				return;// nothing to process
			}
		}else {
			uuid = ADMIN_LOCK_UUID;
			paymentRequest = adminPaymentRequestOpt.get();
		}
		
		try {

			List<FseTransaction> transactions = transactionHistoryService.getTransactionsBetweenDates(
					paymentRequest.getFromClassicAddress(), paymentRequest.getCreateDate(), new Date());
			
			PaymentRequestEnt paymentRequestAfterVerify = airdropVerificationService.verifyDropComplete(paymentRequest, transactions,
					false);

			if (DropRequestStatus.COMPLETE == paymentRequestAfterVerify.getStatus()) {
				return;
			}


			List<DropRecipientEnt> recipients = dropRecipientRepo
					.findAll(Example.of(DropRecipientEnt.builder().dropRequestId(paymentRequest.getId()).build())).stream()
					.filter(r -> DropRecipientStatus.VERIFIED != r.getStatus() ).collect(Collectors.toList());
			log.info("STARTED Airdrop for UUID: {} paymentRequest ID: {} total eligible: {}",
					paymentRequest.getLockUuid(), paymentRequest.getId(), recipients.size());

			AtomicInteger count = new AtomicInteger();
			for (DropRecipientEnt recipient : recipients) {

				count.incrementAndGet();
				Thread.sleep(200);//as measured with legacy implementation.  4000 tx in ~ 10 minutes
				handlePayment(paymentRequest, recipient, count);

			}

			Thread.sleep(30000);// wait a minute. Make sure anything that was queued is done
			List<FseTransaction> transactionsAfterProcessing = transactionHistoryService.getTransactionsBetweenDates(
					paymentRequest.getFromClassicAddress(), paymentRequest.getCreateDate(), new Date());
			
			PaymentRequestEnt finalizedPayment = airdropVerificationService.verifyDropComplete(paymentRequest, transactionsAfterProcessing, true);

			log.info("Airdrop scheduler completed for {}", finalizedPayment);
		} catch (Exception e) {
			log.error("Error verifying drop ", e);
			airdropVerificationService.resetUuidForProcessing(paymentRequest);
		}
	}

	private DropRecipientEnt handlePayment(PaymentRequestEnt p, DropRecipientEnt recipient, AtomicInteger count) {

		count.incrementAndGet();
		// check if job canceled
		if (cancelDropRequestRepo.exists(Example.of(CancelDropRequestEnt.builder().dropRequestId(p.getId()).build()))) {
			try {
				p.setFailReason(REASON_CANCEL_BY_USER);
				p.setStatus(DropRequestStatus.REJECTED);
				p.setFromPrivateKey(null);
				p.setFromSigningPublicKey(null);
				paymentRequestRepo.save(p);

				return recipient;
			} catch (Exception e) {
				log.error("Error Job Cancelled by user - " + p.getId(), e);
			}
		}

		FsePaymentRequest payment = FsePaymentRequest.builder()
				.trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress())
				.currencyName(p.getCurrencyNameForProcess()).amount(p.getAmount()).agreeFee(true)
				.fromClassicAddress(p.getFromClassicAddress()).fromPrivateKey(p.getFromPrivateKey())
				.paymentType(p.getPaymentType()).maxXrpFeePerTransaction(p.getMaxXrpFeePerTransaction())
				.useBlacklist(p.getUseBlacklist())
				.memo(p.getMemo())
				.fromSigningPublicKey(p.getFromSigningPublicKey()).build();
		DropRecipientEnt paidRecipient = paymentService.allowPayment(payment, recipient);

		if ("tefBAD_AUTH".equals(paidRecipient.getCode())) {
			log.info("Rejecting remaining payments due to bad code on recipient" + paidRecipient);

			p.setFailReason("Public Signing or Private key incorrect for from address");
			p.setStatus(DropRequestStatus.REJECTED);
			p.setFromPrivateKey(null);
			p.setFromSigningPublicKey(null);
			paymentRequestRepo.save(p);
		}

		return paidRecipient;
	}

}
