package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableList;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResults;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

@RestController
public class TrustlinePaymentControllerTest {

	@Mock
	private XrplService xrplService;

	private TrustlinePaymentController sut;


	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		sut = new TrustlinePaymentController();
		sut.xrplService = xrplService;
	}


	@Test
	public void testPayment() {
		
		FsePaymentResult expected = fsepaymentResult();
		
		FsePaymentRequest paymentRequest = Mockito.mock(FsePaymentRequest.class);
		Mockito.when(xrplService.sendFsePayment(paymentRequest)).thenReturn(ImmutableList.of(expected));

		FsePaymentResults actual = sut.payment(paymentRequest);

		Assertions.assertEquals(expected, actual.getResults().get(0));
	}

	private FsePaymentResult fsepaymentResult() {
		return Mockito.mock(FsePaymentResult.class);
	}

	@Test
	public void testPaymentTrustlines() {

		FsePaymentTrustlinesRequest paymentRequest = Mockito.mock(FsePaymentTrustlinesRequest.class);

		FsePaymentResults expected = FsePaymentResults.builder().transactionCount(1).build();
		
		Mockito.when(xrplService.sendFsePaymentToTrustlines(paymentRequest))
				.thenReturn(expected);

		FsePaymentResults actual = sut.paymentTrustlines(paymentRequest);
		
		Assertions.assertEquals(expected, actual);
	}
}