package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropScheduleEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropScheduleRunEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropFrequency;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRequestStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropScheduleStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropType;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropRecipientRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropScheduleRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropScheduleRunRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.EmailService;
import com.strategyengine.xrpl.fsedistributionservice.service.ValidationService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class AirDropSchedulerImpl {

	@VisibleForTesting
	@Autowired
	protected DropScheduleRepo dropScheduleRepo;

	@VisibleForTesting
	@Autowired
	protected DropScheduleRunRepo dropScheduleRunRepo;

	@VisibleForTesting
	@Autowired
	protected PaymentRequestRepo paymentRequestRepo;

	@VisibleForTesting
	@Autowired
	protected DropRecipientRepo dropRecipientRepo;

	@VisibleForTesting
	@Autowired
	protected XrplService xrplService;

	@VisibleForTesting
	@Autowired
	protected EmailService emailService;

	@Autowired
	protected ValidationService validationService;

	// runs every 5 minutes
	@Scheduled(fixedDelay = 1000 * 60 * 5) // @Scheduled(fixedDelay = 1000 * 60 * 5)
	public void checkScheduledDrops() {
		String uuid = UUID.randomUUID().toString();

		try {

			if (!lockActiveSchedules(uuid)) {
				dropScheduleRepo.removeUuid(uuid);
				return;
			}

			List<DropScheduleEnt> schedules = dropScheduleRepo.findAll(
					Example.of(DropScheduleEnt.builder().dropScheduleStatus(DropScheduleStatus.ACTIVE).build()));

			schedules.stream().forEach(s -> handleSchedule(s));

		} finally {
			dropScheduleRepo.removeUuid(uuid);
		}
	}

	private void handleSchedule(DropScheduleEnt sched) {
		try {
			// original schedule as saved in drop_request with a status as SCHEDULED
			Optional<PaymentRequestEnt> scheduledPaymentReq = paymentRequestRepo.findOne(Example.of(PaymentRequestEnt
					.builder().id(sched.getDropRequestId()).status(DropRequestStatus.SCHEDULED).build()));

			if (scheduledPaymentReq.isEmpty()) {
				log.error(
						"NEEDS IMMEDIATE ATTENTION:  Schedule exists, but no scheduled payment request.  This should have been created in xrplservice! "
								+ sched);
				return;
			}

			try {
				validationService.validateAirdropNotAlreadyQueuedForFromAddress(
						scheduledPaymentReq.get().getFromClassicAddress());
				validationService.validateAirdropNotAlreadyQueuedForIssuer(
						scheduledPaymentReq.get().getTrustlineIssuerClassicAddress());

			} catch (Exception e) {
				log.info("Waiting to start scheduled airdrop: " + e.getMessage());
				return;
			}
			List<DropScheduleRunEnt> scheduleRuns = dropScheduleRunRepo
					.findAll(Example.of(DropScheduleRunEnt.builder().dropScheduleId(sched.getId()).build()));

			if (sched.getRepeatUntilDate().before(new Date())) {
				markScheduleComplete(sched, scheduledPaymentReq, scheduleRuns);
				return;
			}

			// there will always be at least on scheduled run in drop_request since it is
			// inserted when requested
			DropScheduleRunEnt latestScheduledRun = getLatestRun(scheduleRuns);

			Date schedStartTime = scheduledPaymentReq.get().getStartTime();

			if (latestScheduledRun == null) {
				// this scheduled drop has never been run
				if (schedStartTime.before(now().getTime())) {
					runSchedule(sched, scheduledPaymentReq.get());
				}
				return;
			}

			Optional<PaymentRequestEnt> latestPaymentReqRun = paymentRequestRepo
					.findOne(Example.of(PaymentRequestEnt.builder().id(latestScheduledRun.getDropRequestId()).build()));

			if (DropRequestStatus.REJECTED.equals(latestPaymentReqRun.get().getStatus())) {
				// since the last run failed, this schedule is being terminated
				markScheduleCompleteRejected(sched, latestPaymentReqRun.get());
				return;

			}

			if (shouldRunDropScheduleNow(schedStartTime, latestPaymentReqRun.get().getCreateDate(),
					sched.getFrequency())) {
				runSchedule(sched, scheduledPaymentReq.get());
			}
		} catch (Exception e) {
			log.error("Error trying to run scheduled airdrop " + sched, e);
		}
	}

	private DropScheduleRunEnt runSchedule(DropScheduleEnt sched, PaymentRequestEnt p) {

		PaymentRequestEnt newPaymentRequest = null;
		if (DropType.GLOBALID_SPECIFICADDRESSES.equals(p.getDropType())
				|| DropType.SPECIFICADDRESSES.equals(p.getDropType())) {

			List<String> toClassicAddresses = dropRecipientRepo
					.findAll(Example.of(DropRecipientEnt.builder().dropRequestId(p.getId()).build())).stream()
					.map(d -> d.getAddress()).collect(Collectors.toList());

			newPaymentRequest = xrplService.sendFsePayment(FsePaymentRequest.builder().agreeFee(true)
					.amount(p.getAmount()).currencyName(p.getCurrencyName())
					.fromClassicAddress(p.getFromClassicAddress()).fromPrivateKey(p.getFromPrivateKey())
					.fromSigningPublicKey(p.getFromSigningPublicKey())
					.globalIdVerified(DropType.GLOBALID_SPECIFICADDRESSES == p.getDropType())
					.maxXrpFeePerTransaction(p.getMaxXrpFeePerTransaction()).nftIssuingAddress(p.getNftIssuerAddress())
					.nftTaxon(p.getNftTaxon()).paymentType(p.getPaymentType()).retryOfId(p.getRetryOfId())
					.snapshotCurrencyName(p.getSnapshotCurrencyName())
					.snapshotTrustlineIssuerClassicAddress(p.getSnapshotTrustlineIssuerClassicAddress())
					.toClassicAddresses(toClassicAddresses)
					.trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress()).email(p.getContactEmail())
					.useBlacklist(p.getUseBlacklist()).build());
		} else {

			newPaymentRequest = xrplService.sendFsePaymentToTrustlines(
					FsePaymentTrustlinesRequest.builder().maxBalance(new BigDecimal(p.getMaxBalance()).doubleValue())
							.maximumTrustlines(p.getMaximumTrustlines())
							.minBalance(new BigDecimal(p.getMinBalance()).doubleValue())
							.newTrustlinesOnly(p.getNewTrustlinesOnly()).agreeFee(true).amount(p.getAmount())
							.currencyName(p.getCurrencyName()).fromClassicAddress(p.getFromClassicAddress())
							.fromPrivateKey(p.getFromPrivateKey()).fromSigningPublicKey(p.getFromSigningPublicKey())
							.globalIdVerified(DropType.GLOBALID == p.getDropType())
							.maxXrpFeePerTransaction(p.getMaxXrpFeePerTransaction()).paymentType(p.getPaymentType())
							.retryOfId(p.getRetryOfId()).snapshotCurrencyName(p.getSnapshotCurrencyName())
							.snapshotTrustlineIssuerClassicAddress(p.getSnapshotTrustlineIssuerClassicAddress())
							.trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress())
							.email(p.getContactEmail()).useBlacklist(p.getUseBlacklist()).build(),
					null);
		}

		if (newPaymentRequest.getContactEmail() != null) {
			emailService.sendEmail(newPaymentRequest.getContactEmail(),
					"strategyengine.one Airdrop Scheduled -" + newPaymentRequest.getId(),
					"An airdrop has been scheduled to begin and can be found here: <a href='https://strategyengine.one/#/airdropdetails?dropRequestId="
							+ newPaymentRequest.getId()
							+ "'>Drop Details</a>.  This was triggered by the following  <a href='https://strategyengine.one/#/airdropdetails?dropRequestId="
							+ sched.getDropRequestId() + "'>Airdrop Schedule</a>");
		}

		return dropScheduleRunRepo.save(DropScheduleRunEnt.builder().createDate(new Date())
				.dropRequestId(newPaymentRequest.getId()).dropScheduleId(sched.getId()).build());

	}

	protected boolean shouldRunDropScheduleNow(Date schedStartTime, Date lastPaymentDate, DropFrequency frequency) {
		// a drop should have occurred after this date
		Date lastTimeDropShouldHaveRun = getNextRun(schedStartTime, frequency);

		if (lastTimeDropShouldHaveRun != null && lastTimeDropShouldHaveRun.after(lastPaymentDate)
				&& lastTimeDropShouldHaveRun.before(now().getTime())) {

			return true;
		}

		return false;
	}

	// TODO check if time goes past current year week into next year week that the
	// date is still correct
	protected Date getNextRun(Date schedStartTime, DropFrequency frequency) {

		switch (frequency) {
		case ANNUALLY:
			return getClosestTimeToNow(schedStartTime, Calendar.YEAR);
		case DAILY:
			return getClosestTimeToNow(schedStartTime, Calendar.DATE);
		case MONTHLY:
			return getClosestTimeToNow(schedStartTime, Calendar.MONTH);
		case WEEKLY:
			return getClosestTimeToNow(schedStartTime, Calendar.WEEK_OF_YEAR);
		default:
			break;
		}
		return null;
	}

	protected Date getClosestTimeToNow(Date schedStartTime, int timeUnit) {
		Calendar now = now();
		Calendar t = Calendar.getInstance();
		t.setTime(schedStartTime);

		while (t.before(now)) {
			t.add(timeUnit, 1);
			if (t.after(now)) {
				t.add(timeUnit, -1);
				return t.getTime();
			}

		}

		return null;
	}

	protected Calendar now() {
		return Calendar.getInstance();
	}

	private void markScheduleCompleteRejected(DropScheduleEnt sched, PaymentRequestEnt latestPaymentReq) {

		DropScheduleEnt schedComplete = dropScheduleRepo
				.save(sched.toBuilder().dropScheduleStatus(DropScheduleStatus.REJECTED).build());

		if (latestPaymentReq.getContactEmail() != null) {
			emailService.sendEmail(latestPaymentReq.getContactEmail(),
					"strategyengine.one Airdrop Schedule Terminated -" + latestPaymentReq.getId(),
					"A <a href='https://strategyengine.one/#/airdropdetails?dropRequestId=" + sched.getDropRequestId()
							+ "'>scheduled airdrop</a> has been terminated.  The most recent scheduled airdrop has been rejected.  "
							+ "Details for the rejection can be found here: <a href='https://strategyengine.one/#/airdropdetails?dropRequestId="
							+ latestPaymentReq.getId() + "'>Drop Details</a>");
		}

	}

	private void markScheduleComplete(DropScheduleEnt sched, Optional<PaymentRequestEnt> scheduledPaymentReq,
			List<DropScheduleRunEnt> scheduleRuns) {

		DropScheduleEnt schedComplete = dropScheduleRepo
				.save(sched.toBuilder().dropScheduleStatus(DropScheduleStatus.COMPLETE).build());

		if (scheduledPaymentReq.isEmpty() || scheduleRuns.isEmpty()) {
			return;
		}

		StringBuilder sb = new StringBuilder();
		for (DropScheduleRunEnt run : scheduleRuns) {
			sb.append("<br><a href='https://strategyengine.one/#/airdropdetails?dropRequestId="
					+ scheduledPaymentReq.get().getId() + "'>Drop Details</a>");
		}

		if (scheduledPaymentReq.get().getContactEmail() != null) {
			emailService.sendEmail(scheduledPaymentReq.get().getContactEmail(),
					"strategyengine.one Airdrop Schedule Completed",
					"Your scheduled airdrops have been completed. Schedule details can be found here: <a href='https://strategyengine.one/#/airdropdetails?dropRequestId="
							+ sched.getDropRequestId() + "'>Schedule Details</a> and detailed runs below:"
							+ sb.toString());
		}

	}

	private DropScheduleRunEnt getLatestRun(List<DropScheduleRunEnt> scheduleRuns) {

		DropScheduleRunEnt latestRun = null;

		for (DropScheduleRunEnt run : scheduleRuns) {
			if (latestRun == null) {
				latestRun = run;
			} else if (latestRun.getCreateDate().after(run.getCreateDate())) {
				latestRun = run;
			}
		}

		return latestRun;
	}

	private boolean lockActiveSchedules(String uuid) {

		// try to flag one or more payment requests to start work on it
		dropScheduleRepo.updateUuid(uuid);

		Optional<DropScheduleEnt> schedule = dropScheduleRepo
				.findOne(Example.of(DropScheduleEnt.builder().dropScheduleStatus(DropScheduleStatus.ACTIVE).build()));

		if (schedule.isPresent()) {

			return schedule.get().getLockUuid().equals(uuid);
		}
		return false;

	}

}
