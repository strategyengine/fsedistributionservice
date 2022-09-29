package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRecipientStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRequestStatus;
import com.strategyengine.xrpl.fsedistributionservice.model.PaymentAmountChange;
import com.strategyengine.xrpl.fsedistributionservice.model.PaymentsChange;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropRecipientRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
import com.strategyengine.xrpl.fsedistributionservice.service.PaymentsChangeService;
import com.strategyengine.xrpl.fsedistributionservice.service.ValidationService;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class PaymentsChangeServiceImpl implements PaymentsChangeService {

	@Autowired
	protected DropRecipientRepo dropRecipientRepo;

	@VisibleForTesting
	@Autowired
	protected PaymentRequestRepo paymentRequestRepo;
	
	@VisibleForTesting
	@Autowired
	protected ValidationService validationService;
	
	@Override
	public void removeRecipient(Long dropRecipId, String privateKey) {
		
		log.info("Delete recipient " + dropRecipId);
		
		Optional<DropRecipientEnt> recip = dropRecipientRepo.findById(dropRecipId);
		
		if(recip.isEmpty()) {
			throw new BadRequestException("No recipient found.  Could not remove");
		}
		
		validateDropRequestChange(recip.get().getDropRequestId(), privateKey);
		
		Optional<PaymentRequestEnt> dropRequest = paymentRequestRepo.findById(recip.get().getDropRequestId());
		
		if (dropRequest.isEmpty()) {
			throw new BadRequestException("Could not find drop request for id " + recip.get().getDropRequestId());
		}
		
		if (DropRequestStatus.PENDING_REVIEW!=dropRequest.get().getStatus()) {
			throw new BadRequestException("Payment changes are only allowed on airdrops in pending review status");
		}
		
		dropRecipientRepo.deleteById(dropRecipId);
	}

	@Override
	public void updatePaymentAmounts(final PaymentsChange paymentsChange) {
		log.info("Update payment amounts " + paymentsChange.getPaymentAmountChanges());

		validateDropRequestChange(paymentsChange.getDropRequestId(), paymentsChange.getPrivateKey());
		
		paymentsChange.getPaymentAmountChanges().stream().forEach(p -> validatePaymemtUpdate(p));

		List<DropRecipientEnt> recipients = dropRecipientRepo.findAll(
				Example.of(DropRecipientEnt.builder().dropRequestId(paymentsChange.getDropRequestId()).build()));

		updateExistingRecipients(recipients, paymentsChange);

		addNewPayments(recipients, paymentsChange, paymentsChange.getDropRequestId());

	}

	private void validatePaymemtUpdate(PaymentAmountChange p) {
		if(!validationService.isValidClassicAddress(p.getToClassicAddress())) {
			throw new BadRequestException("Invalid address found " + p.getToClassicAddress());
		}
		
		try {
			new BigDecimal(p.getAmount());
		}catch(Exception e) {
			throw new BadRequestException("Invalid amount for address " + p.getToClassicAddress() + " " + p.getAmount());
		}
	}

	private void validateDropRequestChange(Long dropRequestId, String privateKey) {
		Optional<PaymentRequestEnt> dropRequest = paymentRequestRepo.findById(dropRequestId);

		if (dropRequest.isEmpty()) {
			throw new BadRequestException("Could not find drop request for id " + dropRequestId);
		}
		
		if (DropRequestStatus.PENDING_REVIEW!=dropRequest.get().getStatus()) {
			throw new BadRequestException("Payment changes are only allowed on airdrops in pending review status");
		}
		
		if(!dropRequest.get().getFromPrivateKey().equals(privateKey)) {
			throw new BadRequestException("Private key does not match original key used to create drop.   Private key is for the from / sending address not the issuing address.");
		}
		
	}

	private void addNewPayments(List<DropRecipientEnt> recipients, PaymentsChange paymentsChange, Long dropId) {
		
		List<PaymentAmountChange> newRecipients = paymentsChange.getPaymentAmountChanges().stream()
				.filter(p -> !isCurrentRecipient(p, recipients)).collect(Collectors.toList());
		
		List<DropRecipientEnt> newRecipEnts = newRecipients.stream().map(r -> DropRecipientEnt.builder().createDate(new Date()).updateDate(new Date())
				.address(r.getToClassicAddress().trim()).dropRequestId(dropId).payAmount(r.getAmount()).status(DropRecipientStatus.QUEUED).build()).collect(Collectors.toList());
		
		dropRecipientRepo.saveAll(newRecipEnts);
		
		
	}

	private boolean isCurrentRecipient(PaymentAmountChange p, List<DropRecipientEnt> recipients) {
		return recipients.stream().filter(r -> r.getAddress().equals(p.getToClassicAddress().trim())).findAny().isPresent();
	}

	private void updateExistingRecipients(List<DropRecipientEnt> recipients, PaymentsChange paymentsChange) {
		List<DropRecipientEnt> updatedRecips = recipients.stream().map(r -> updateRecipientAmount(r, paymentsChange))
				.filter(r -> r.isPresent()).map(r -> r.get()).collect(Collectors.toList());

		dropRecipientRepo.saveAll(updatedRecips);

	}

	private Optional<DropRecipientEnt> updateRecipientAmount(DropRecipientEnt recipient,
			PaymentsChange paymentsChange) {

		Optional<PaymentAmountChange> change = paymentsChange.getPaymentAmountChanges().stream()
				.filter(p -> StringUtils.hasLength(p.getAmount()) && StringUtils.hasLength(p.getToClassicAddress())
						&& recipient.getAddress().equals(p.getToClassicAddress().trim()))
				.findAny();

		if (change.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(recipient.toBuilder().payAmount(change.get().getAmount()).updateDate(new Date()).build());

	}

}
