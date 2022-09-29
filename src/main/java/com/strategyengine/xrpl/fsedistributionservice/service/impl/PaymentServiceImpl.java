package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRecipientStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.PaymentType;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropRecipientRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.PaymentService;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class PaymentServiceImpl implements PaymentService {

	@VisibleForTesting
	@Autowired
	protected XrplClientService xrplClientService;

	@VisibleForTesting
	@Autowired
	protected DropRecipientRepo dropRecipientRepo;

	//max in the db on any successful drop
	private int MAX_ATTEMPTS = 10;

	@Transactional(isolation = Isolation.READ_UNCOMMITTED)
	@Override
	public DropRecipientEnt allowPayment(FsePaymentRequest paymentRequest, DropRecipientEnt recipient) {

		if (!shouldAttempt(recipient.getStatus())) {
			return recipient;
		}
		
		if(recipient.getPayAmount()==null) {
			if(PaymentType.PROPORTIONAL == paymentRequest.getPaymentType()) {
				throw new RuntimeException("Cannot process non FLAT payment if recipient pay amount is blank! " + recipient);
			}
			//for backward compatibility since airdrops created before this field will have null in recipient.payAmount
			dropRecipientRepo
				.save(recipient.toBuilder().payAmount(paymentRequest.getAmount()).status(DropRecipientStatus.SENDING).updateDate(new Date()).build());
		}else {

			dropRecipientRepo
				.save(recipient.toBuilder().status(DropRecipientStatus.SENDING).updateDate(new Date()).build());
		
		}
		DropRecipientEnt result = allowPaymentNoPersist(paymentRequest, recipient);
		int attempt = recipient.getRetryAttempt()==null ? 1 : recipient.getRetryAttempt() + 1;

		final DropRecipientStatus status;

		if (attempt >= MAX_ATTEMPTS) {
			status = DropRecipientStatus.FAILED;

		} else {

			status = DropRecipientStatus.SENDING == result.getStatus() ? DropRecipientStatus.QUEUED
					: result.getStatus();
		}

		DropRecipientEnt recipientUpdated = result.toBuilder().status(status).updateDate(new Date())
				.retryAttempt(attempt).build();

		log.info("{} Recipient payment result ", recipientUpdated);
		return dropRecipientRepo.save(recipientUpdated);

	}

	private boolean shouldAttempt(DropRecipientStatus status) {
		if (DropRecipientStatus.VERIFIED == status || DropRecipientStatus.FAILED == status) {
			return false;
		}
		return true;
	}

	@Override
	public DropRecipientEnt allowPaymentNoPersist(FsePaymentRequest paymentRequest, DropRecipientEnt dropRecipientEnt) {

		try {

			DropRecipientEnt result = xrplClientService.sendFSEPayment(paymentRequest, dropRecipientEnt);
			
			return result;
		} catch (Exception e) {
			log.error("Error sending payment to " + paymentRequest, e);
		}

		return null;

	}

}
