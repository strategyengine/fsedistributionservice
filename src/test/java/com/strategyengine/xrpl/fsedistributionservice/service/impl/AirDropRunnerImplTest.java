package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.PaymentService;

public class AirDropRunnerImplTest {

	private AirDropRunnerImpl sut;

	@Mock
	private PaymentRequestRepo paymentRequestRepo;

	@Mock
	private PaymentService paymentService;

	private Date now = new Date();

	@BeforeEach
	public void setup() throws Exception {

		MockitoAnnotations.openMocks(this);
		sut = new AirDropRunnerImpl();
		sut.paymentRequestRepo = paymentRequestRepo;
		sut.paymentService = paymentService;

	}

	@Test
	public void test() {

//TOOD
	}

}
