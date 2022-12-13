package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropScheduleEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropScheduleRunEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropFrequency;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRequestStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropScheduleStatus;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropScheduleRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropScheduleRunRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;

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
	
	// runs every 6 minutes
	@Scheduled(fixedDelay = 360000)
	public void checkScheduledDrops() {

		String uuid = UUID.randomUUID().toString();

		if (!lockActiveSchedules(uuid)) {
			return;
		}

		List<DropScheduleEnt> schedules = dropScheduleRepo
				.findAll(Example.of(DropScheduleEnt.builder().dropScheduleStatus(DropScheduleStatus.ACTIVE).build()));

		schedules.stream().forEach(s -> handleSchedule(s));

		dropScheduleRepo.removeUuid(uuid);

	}

	private void handleSchedule(DropScheduleEnt sched) {

		if (sched.getRepeatUntilDate().after(new Date())) {
			markScheduleComplete(sched);
			return; 
		}

		List<DropScheduleRunEnt> scheduleRuns = dropScheduleRunRepo
				.findAll(Example.of(DropScheduleRunEnt.builder().dropScheduleId(sched.getId()).build()));

		DropScheduleRunEnt latestRun = getLatestRun(scheduleRuns);
		
		Optional<PaymentRequestEnt> latestPaymentReq = paymentRequestRepo.findOne(Example.of(PaymentRequestEnt.builder().id(latestRun.getDropRequestId()).build()));
		
		if(DropRequestStatus.COMPLETE.equals(latestPaymentReq.get().getStatus())){
			Date schedStartTime = latestPaymentReq.get().getStartTime();
			
			Date nextRun = getNextRun(schedStartTime, sched.getFrequency());
			
			if(nextRun!=null && nextRun.after(latestPaymentReq.get().getCreateDate())) {
				
				//TODO create a new airdrop
				
				//TODO create a new schedule run
			}
			
		}

		if(DropRequestStatus.REJECTED.equals(latestPaymentReq.get().getStatus())){
			markScheduleCompleteRejected(sched);
			return;
			
		}


	}
	//TODO check if time goes past current year week into next year week that the date is still correct
	private Date getNextRun(Date schedStartTime, DropFrequency frequency) {
		
		switch (frequency){
			case ONCE: return null;
		case ANNUALLY: return getClosestTimeToNow(schedStartTime, Calendar.YEAR);
		case DAILY:return getClosestTimeToNow(schedStartTime, Calendar.DATE);
		case MONTHLY:return getClosestTimeToNow(schedStartTime, Calendar.MONTH);
		case WEEKLY:return getClosestTimeToNow(schedStartTime, Calendar.WEEK_OF_YEAR);
		default:
			break;
		}
		return null;
	}

	protected Date getClosestTimeToNow(Date schedStartTime, int timeUnit) {
		Calendar now = Calendar.getInstance();
		Calendar t = Calendar.getInstance();
		t.setTime(schedStartTime);
		
		
		while(t.before(now)) {
			t.add(timeUnit, 1);
			if(t.after(now)) {
				t.add(timeUnit, -1);
				return t.getTime();
			}
			
		}
		
		return null;
	}

	private void markScheduleCompleteRejected(DropScheduleEnt sched) {
		
		DropScheduleEnt schedComplete = dropScheduleRepo.save(sched.toBuilder().dropScheduleStatus(DropScheduleStatus.COMPLETE).build());
		
		//TODO send email that schedule is complete and rejected
		
	}

	private void markScheduleComplete(DropScheduleEnt sched) {
		
		DropScheduleEnt schedComplete = dropScheduleRepo.save(sched.toBuilder().dropScheduleStatus(DropScheduleStatus.COMPLETE).build());
		
		//TODO send email that schedule is complete
		
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
