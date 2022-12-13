package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropScheduleEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropScheduleRunEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.SummaryResult;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropFrequency;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRequestStatus;
import com.strategyengine.xrpl.fsedistributionservice.model.AirdropStatus;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropRecipientRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropScheduleRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropScheduleRunRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropSummaryService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class AridropSummaryServiceImpl implements AirdropSummaryService {

	@VisibleForTesting
	@Autowired
	protected DropRecipientRepo dropRecipientRepo;

	@VisibleForTesting
	@Autowired
	protected PaymentRequestRepo paymentRequestRepo;

	@VisibleForTesting
	@Autowired
	protected DropScheduleRepo dropScheduleRepo;
	
	@VisibleForTesting
	@Autowired
	protected DropScheduleRunRepo dropScheduleRunRepo;
	
	@Override
	public List<AirdropStatus> getAirdrops(String issuingAddress) {
		List<PaymentRequestEnt> paymentRequests = paymentRequestRepo.findAll(
				Example.of(PaymentRequestEnt.builder().trustlineIssuerClassicAddress(issuingAddress).build()),
				Sort.by(Direction.DESC, "createDate"));

		List<AirdropStatus> drops = new ArrayList<>();
		drops.addAll(paymentRequests.stream().map(p -> convert(p)).collect(Collectors.toList()));
		
		List<PaymentRequestEnt> paymentRequestsSnap = paymentRequestRepo.findAll(
				Example.of(PaymentRequestEnt.builder().snapshotTrustlineIssuerClassicAddress(issuingAddress).build()),
				Sort.by(Direction.DESC, "createDate"));
		
		drops.addAll(paymentRequestsSnap.stream().filter(p -> !paymentRequests.contains(p)).map(p -> convert(p)).collect(Collectors.toList()));
		
		return drops;

	}

	private AirdropStatus convert(PaymentRequestEnt p) {
		
		List<DropScheduleRunEnt> dropRuns = dropScheduleRunRepo.findAll(Example.of(DropScheduleRunEnt.builder().dropRequestId(p.getId()).build()));
		
		Date repeatUntilDate = null;
		DropFrequency frequency = DropFrequency.ONCE;
		if(!dropRuns.isEmpty()) {
			
			Optional<DropScheduleEnt> dropSchedule = dropScheduleRepo.findById(dropRuns.get(0).getDropRequestId());
			
			if(dropSchedule.isPresent()) {
				repeatUntilDate = dropSchedule.get().getRepeatUntilDate();
				frequency = dropSchedule.get().getFrequency();
			}
		}
		
		return AirdropStatus.builder().amount(p.getAmount()).createDate(p.getCreateDate())
				.currencyName(p.getCurrencyName()).currencyNameForProcess(p.getCurrencyNameForProcess())
				.dropType(p.getDropType()).failReason(p.getFailReason()).fromClassicAddress(p.getFromClassicAddress())
				.id(p.getId()).maximumTrustlines(p.getMaximumTrustlines()).newTrustlinesOnly(p.getNewTrustlinesOnly())
				.useBlacklist(p.getUseBlacklist())
				.status(p.getStatus()).trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress())
				.paymentType(p.getPaymentType())
				.minBalance(p.getMinBalance()).maxBalance(p.getMaxBalance())
				.snapshotTrustlineIssuerClassicAddress(p.getSnapshotTrustlineIssuerClassicAddress())
				.snapshotCurrencyName(p.getSnapshotCurrencyName())
				.maxXrpFeePerTransaction(p.getMaxXrpFeePerTransaction()).updateDate(p.getUpdateDate())
				.nftIssuingAddress(p.getNftIssuerAddress())
				.startTime(p.getStartTime())
				.frequency(frequency)
				.repeatUntilDate(repeatUntilDate)
				.nftTaxon(String.valueOf(p.getNftTaxon())).build();
	}

	@Override
	public AirdropStatus getAirdropDetails(Long paymentRequestId) {

		Optional<PaymentRequestEnt> paymentRequest = paymentRequestRepo
				.findOne(Example.of(PaymentRequestEnt.builder().id(paymentRequestId).build()));

		if (paymentRequest.isEmpty()) {
			return null;
		}

		AirdropStatus status = convert(paymentRequest.get());

		List<DropRecipientEnt> recipients = dropRecipientRepo.findAll(
				Example.of(DropRecipientEnt.builder().dropRequestId(paymentRequestId).build()),
				Sort.by(Direction.DESC, "status"));

		long totalBlacklisted = recipients.stream().filter(r -> "blacklistedFail".equals(r.getCode())).count();
		status.setResults(recipients.stream().filter(r -> !"blacklistedFail".equals(r.getCode()))
				.map(r -> FsePaymentResult.builder().id(r.getId()).status(r.getStatus()).classicAddress(r.getAddress())
						.reason(r.getFailReason()).responseCode(r.getCode())
						.paymentAmount(r.getPayAmount()).snapshotBalance(r.getSnapshotBalance())
						.nftOwned(r.getOwnedNftId())
						.build())

				.collect(Collectors.toList()));

		status.setTotalBlacklisted(totalBlacklisted);
		return status;

	}

	@Cacheable("activedropsCache")
	@Override
	public List<AirdropStatus> getIncompleteAirdrops() {
		List<PaymentRequestEnt> incomplete = paymentRequestRepo.findAll(
				
				Example.of(PaymentRequestEnt.builder().status(DropRequestStatus.IN_PROGRESS).build()),
				Sort.by("createDate"));
		incomplete.addAll(paymentRequestRepo.findAll(
				Example.of(PaymentRequestEnt.builder().status(DropRequestStatus.QUEUED).build()),
				Sort.by("createDate")));

		incomplete.addAll(paymentRequestRepo.findAll(
				Example.of(PaymentRequestEnt.builder().status(DropRequestStatus.POPULATING_ADDRESSES).build()),
				Sort.by("createDate")));
		
		incomplete.addAll(paymentRequestRepo.findAll(
				Example.of(PaymentRequestEnt.builder().status(DropRequestStatus.PENDING_REVIEW).build()),
				Sort.by("createDate")));		
		
		return incomplete.stream().map(p -> convert(p)).collect(Collectors.toList());
	}

	@Override
	public List<AirdropStatus> getAirdrops() {

		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, -30);
		List<PaymentRequestEnt> drops = paymentRequestRepo.findByStatusAfterDate(DropRequestStatus.COMPLETE, c.getTime());

		
		//TODO improve this query 
		final List<SummaryResult> summaries = dropRecipientRepo.countsByPaymentId(drops.stream().map(d -> d.getId()).collect(Collectors.toList()));
		
		List<AirdropStatus> status = drops.stream().map(p -> convertWithTotals(p, summaries)).collect(Collectors.toList());

		return status;
	}

	private AirdropStatus convertWithTotals(PaymentRequestEnt p, List<SummaryResult> summaries) {

		AirdropStatus status = convert(p);
		status.setTotalRecipients(summaries.stream().filter(s -> s.getId().equals(status.getId())).map(s -> s.getCount()).collect(Collectors.summingLong(Long::longValue))
				);

		return status;

	}

}
