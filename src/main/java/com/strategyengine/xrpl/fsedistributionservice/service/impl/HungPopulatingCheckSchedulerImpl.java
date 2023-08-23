package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.hazelcast.internal.util.StringUtil;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRequestStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropType;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropRecipientRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.ConfigService;
import com.strategyengine.xrpl.fsedistributionservice.service.HungPopulatingCheckScheduler;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class HungPopulatingCheckSchedulerImpl implements HungPopulatingCheckScheduler {

	@VisibleForTesting
	@Autowired
	protected DropRecipientRepo dropRecipientRepo;

	@VisibleForTesting
	@Autowired
	protected PaymentRequestRepo paymentRequestRepo;

	@VisibleForTesting
	@Autowired
	protected XrplService xrplService;

	@VisibleForTesting
	@Autowired
	protected ConfigService config;
	
	private String environment = System.getenv("ENV");
	
	// DO NOT MARK THIS METHOD TRANSACTIONAL we need other processes to see the
	// lock_uuid
	// fixedDelay millis - check every 10 minutes for a drop
	@Scheduled(fixedDelay = 600000, initialDelay=600)
	public void findHungPopulating() {

		try {
			
			if(!environment.equals(config.retryHungDropsEnvironment())){
				return;
			}
			
			List<PaymentRequestEnt> payments = paymentRequestRepo.findAll(
					Example.of(PaymentRequestEnt.builder().status(DropRequestStatus.POPULATING_ADDRESSES).build()));

			Calendar yesterday = Calendar.getInstance();
			yesterday.add(Calendar.HOUR, -3);

			Optional<PaymentRequestEnt> hungPopulatingSinceYesterday = payments.stream().filter(
					p -> StringUtil.isNullOrEmpty(p.getLockUuid()) && yesterday.getTime().after(p.getUpdateDate())).findAny();

			if(hungPopulatingSinceYesterday.isPresent()) {
				PaymentRequestEnt resubmitted = resubmit(hungPopulatingSinceYesterday.get());
				log.info("Resubmit result " + resubmitted);
			}

		} catch (Exception e) {
			log.error("Fail when finding hung populating address job", e);
		}

	}

	private PaymentRequestEnt resubmit(PaymentRequestEnt p) {
		if (DropType.SPECIFICADDRESSES.equals(p.getDropType())
				|| DropType.GLOBALID_SPECIFICADDRESSES.equals(p.getDropType())) {
			return submitSpecificAddresses(p);
		}
		return submitTrustlineDrop(p);
	}

	private PaymentRequestEnt submitTrustlineDrop(PaymentRequestEnt p) {


		log.info("Resend populating_address for trustline addresses " + p);
		paymentRequestRepo.save(p.toBuilder().status(DropRequestStatus.REJECTED).failReason("Job was automatically resubmitted").build());
		
		
		PaymentRequestEnt resubmitted = xrplService.sendFsePaymentToTrustlines(FsePaymentTrustlinesRequest.builder()
				.memo(p.getMemo())
				.maxBalance(p.getMaxBalance()!=null? Double.valueOf(p.getMaxBalance()) : null)
				.maximumTrustlines(p.getMaximumTrustlines())
				.minBalance(p.getMinBalance()!=null ? Double.valueOf(p.getMinBalance()) : null)
				.newTrustlinesOnly(p.getNewTrustlinesOnly())
				.agreeFee(true).amount(p.getAmount())
				.currencyName(p.getCurrencyName()).fromClassicAddress(p.getFromClassicAddress())
				.fromPrivateKey(p.getFromPrivateKey()).fromSigningPublicKey(p.getFromSigningPublicKey())
				.globalIdVerified(DropType.GLOBALID.equals(p.getDropType())
						|| DropType.GLOBALID_SPECIFICADDRESSES.equals(p.getDropType()))
				.maxXrpFeePerTransaction(p.getMaxXrpFeePerTransaction())
				.paymentType(p.getPaymentType())
				.retryOfId(p.getRetryOfId()).snapshotCurrencyName(p.getSnapshotCurrencyName())
				.snapshotTrustlineIssuerClassicAddress(p.getSnapshotTrustlineIssuerClassicAddress())
				.trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress())
				.useBlacklist(p.getUseBlacklist()).build(), null);
		
		paymentRequestRepo.save(p.toBuilder().status(DropRequestStatus.REJECTED).failReason("Job was automatically resubmitted with new id " + resubmitted.getId() ).build());
		
		return resubmitted;
		
	}

	private PaymentRequestEnt submitSpecificAddresses(PaymentRequestEnt p) {

		log.info("Resend populating_address for specific addresses " + p);
		paymentRequestRepo.save(p.toBuilder().status(DropRequestStatus.REJECTED).failReason("Job was automatically resubmitted").build());
		
		List<DropRecipientEnt> recipients = dropRecipientRepo
				.findAll(Example.of(DropRecipientEnt.builder().dropRequestId(p.getId()).build()));

		List<String> recipAdds = recipients.stream().map(r -> r.getAddress()).collect(Collectors.toList());
		
		PaymentRequestEnt resubmitted = xrplService.sendFsePayment(FsePaymentRequest.builder().agreeFee(true).amount(p.getAmount())
				.memo(p.getMemo())
				.currencyName(p.getCurrencyName()).fromClassicAddress(p.getFromClassicAddress())
				.fromPrivateKey(p.getFromPrivateKey()).fromSigningPublicKey(p.getFromSigningPublicKey())
				.globalIdVerified(DropType.GLOBALID.equals(p.getDropType())
						|| DropType.GLOBALID_SPECIFICADDRESSES.equals(p.getDropType()))
				.maxXrpFeePerTransaction(p.getMaxXrpFeePerTransaction()).paymentType(p.getPaymentType())
				.retryOfId(p.getRetryOfId()).snapshotCurrencyName(p.getSnapshotCurrencyName())
				.snapshotTrustlineIssuerClassicAddress(p.getSnapshotTrustlineIssuerClassicAddress())
				.toClassicAddresses(recipAdds).trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress())
				.useBlacklist(p.getUseBlacklist()).build());
		
		paymentRequestRepo.save(p.toBuilder().status(DropRequestStatus.REJECTED).failReason("Job was automatically resubmitted with new id " + resubmitted.getId() ).build());
		
		return resubmitted;
		
		
	}

}
