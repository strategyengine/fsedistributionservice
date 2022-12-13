package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropFrequency;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.PaymentService;

public class AirDropSchedulerImplTest {

	private AirDropSchedulerImpl sut;

	@Mock
	private PaymentRequestRepo paymentRequestRepo;

	@Mock
	private PaymentService paymentService;


	@BeforeEach
	public void setup() throws Exception {

		MockitoAnnotations.openMocks(this);
		sut = new AirDropSchedulerImpl();
		sut.paymentRequestRepo = paymentRequestRepo;

	}
	
	
	@Test
	public void testshouldRunDropScheduleNow() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, -1);
		Date schedStartTime = c.getTime();
		Date lastPaymentDate = null;
		DropFrequency frequency = DropFrequency.ANNUALLY;
		
		Assertions.assertTrue(sut.shouldRunDropScheduleNow(schedStartTime, lastPaymentDate, frequency));
	}
	@Test
	public void testgetNextRun_Future_NotReady() {
		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.HOUR, 1);
		
		Date nextRunDate = sut.getNextRun(c.getTime(), DropFrequency.WEEKLY);
		
		Assertions.assertNull(nextRunDate);
		
		
	}
	
	@Test
	public void testgetNextRun_Weeky() {
		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.WEEK_OF_YEAR, -2);
		c.add(Calendar.HOUR, 1);
		
		Date nextRunDate = sut.getNextRun(c.getTime(), DropFrequency.WEEKLY);
		
		Assertions.assertTrue(nextRunDate.before(new Date()));
		
		c.add(Calendar.WEEK_OF_YEAR, 1);
		Assertions.assertEquals(c.getTime(), nextRunDate);
		
	}
	
	@Test
	public void testgetNextRun_Daily() {
		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, -2);
		c.add(Calendar.HOUR, 1);
		
		Date nextRunDate = sut.getNextRun(c.getTime(), DropFrequency.DAILY);
		
		Assertions.assertTrue(nextRunDate.before(new Date()));
		
		c.add(Calendar.DATE, 1);
		Assertions.assertEquals(c.getTime(), nextRunDate);
		
	}
	
	@Test
	public void testgetNextRun_Month() {
		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, -2);
		c.add(Calendar.HOUR, 1);
		
		Date nextRunDate = sut.getNextRun(c.getTime(), DropFrequency.MONTHLY);
		
		Assertions.assertTrue(nextRunDate.before(new Date()));
		
		c.add(Calendar.MONTH, 1);
		Assertions.assertEquals(c.getTime(), nextRunDate);
		
	}
	
	@Test
	public void testgetNextRun_Year() {
		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.YEAR, -2);
		c.add(Calendar.HOUR, 1);
		
		Date nextRunDate = sut.getNextRun(c.getTime(), DropFrequency.ANNUALLY);
		
		Assertions.assertTrue(nextRunDate.before(new Date()));
		
		c.add(Calendar.YEAR, 1);
		Assertions.assertEquals(c.getTime(), nextRunDate);
		
	}
	
	@Test
	public void testgetClosestTimeToNow_2_yearsago_ForMonthlyDrop() {

		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, -2);
		c.add(Calendar.HOUR, 1);
		
		Date dropShouldHaveOccurredOnOrAfter = sut.getClosestTimeToNow(c.getTime(), Calendar.MONTH);
		
		c.add(Calendar.MONTH, 1);
		Assertions.assertEquals(c.getTime(), dropShouldHaveOccurredOnOrAfter);
	}
	
	@Test
	public void testgetClosestTimeToNow_2_yearsago_ForWeeklyDrop_futureWeek() {

		sut = new AirDropSchedulerImpl() {
			@Override
			protected Calendar now() {
				Calendar c = Calendar.getInstance();
				c.add(Calendar.WEEK_OF_YEAR, 10);
				System.out.println("Future Week " + c.getTime());
				return c;
			}
		};
		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.WEEK_OF_YEAR, 1);
		c.add(Calendar.HOUR, 1);
		
		Date dropShouldHaveOccurredOnOrAfter = sut.getClosestTimeToNow(c.getTime(), Calendar.WEEK_OF_YEAR);
		
		c.add(Calendar.WEEK_OF_YEAR, 8);
		Assertions.assertEquals(c.getTime(), dropShouldHaveOccurredOnOrAfter);
	}
	
	@Test
	public void testgetClosestTimeToNow_2_yearsago_ForDailyDrop_DueYesterday() {

		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, -2);
		c.add(Calendar.HOUR, 1);
		
		Date dropShouldHaveOccurredOnOrAfter = sut.getClosestTimeToNow(c.getTime(), Calendar.DATE);
		
		c.add(Calendar.DATE, 1);
		Assertions.assertEquals(c.getTime(), dropShouldHaveOccurredOnOrAfter);
	}
	
	@Test
	public void testgetClosestTimeToNow_2_yearsago_ForDailyDrop_DueNow() {

		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, -2);
		c.add(Calendar.HOUR, -1);
		
		Date dropShouldHaveOccurredOnOrAfter = sut.getClosestTimeToNow(c.getTime(), Calendar.DATE);
		
		c.add(Calendar.DATE, 2);
		Assertions.assertEquals(c.getTime(), dropShouldHaveOccurredOnOrAfter);
	}

	@Test
	public void testgetClosestTimeToNow_2_yearsago_ForAnnualDrop_DueYesterday() {

		Calendar c = Calendar.getInstance();
		c.add(Calendar.YEAR, -2);
		c.add(Calendar.HOUR, -1);
		
		Date dropShouldHaveOccurredOnOrAfter = sut.getClosestTimeToNow(c.getTime(), Calendar.YEAR);
		
		c.add(Calendar.YEAR, 2);
		Assertions.assertEquals(c.getTime(), dropShouldHaveOccurredOnOrAfter);
	}
	
	@Test
	public void testgetClosestTimeToNow_2_yearsago_ForAnnualDrop_DueLastYear_DueAgainTomorrow() {

		Calendar c = Calendar.getInstance();
		c.add(Calendar.YEAR, -2);
		c.add(Calendar.DATE, 1);
		
		Date dropShouldHaveOccurredOnOrAfter = sut.getClosestTimeToNow(c.getTime(), Calendar.YEAR);
		
		c.add(Calendar.YEAR, 1);
		Assertions.assertEquals(c.getTime(), dropShouldHaveOccurredOnOrAfter);
	}
	

	@Test
	public void testgetClosestTimeToNow_Yesterday_ForAnnualDrop() {

		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, -1);
		
		Date dropShouldHaveOccurredOnOrAfter = sut.getClosestTimeToNow(c.getTime(), Calendar.YEAR);
		
		Assertions.assertEquals(c.getTime(), dropShouldHaveOccurredOnOrAfter);
	}
	
	@Test
	public void testgetClosestTimeToNow_FutureDate() {

		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, 1);
		
		Date dropShouldHaveOccurredOnOrAfter = sut.getClosestTimeToNow(c.getTime(), Calendar.YEAR);
		
		Assertions.assertNull(dropShouldHaveOccurredOnOrAfter);
	}

}
