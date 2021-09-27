package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableList;
import com.strategyengine.xrpl.fsedistributionservice.model.AirdropSummary;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

@RestController
public class XrplControllerTest {

	@Mock
	private XrplService xrplService;

	private XrplController sut;

	private String classicAddress = "vincent";

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		sut = new XrplController();
		sut.xrplService = xrplService;
	}

	@Test
	public void testTrustLines() {
		List<FseTrustLine> expected = trustLines();
		
		Mockito.when(xrplService.getTrustLines(classicAddress, Optional.empty(), true, true)).thenReturn(expected);

		List<FseTrustLine> actual = sut.trustLines(classicAddress, null, true);

		Assertions.assertEquals(expected, actual);
	}

	private List<FseTrustLine> trustLines() {
		return ImmutableList.of(Mockito.mock(FseTrustLine.class));
	}

	@Test
	public void testAccountInfo() {
		List<FseAccount> expected = ImmutableList.of( accountInfo());
		
		Mockito.when(xrplService.getAccountInfo(ImmutableList.of(classicAddress))).thenReturn(expected);

		List<FseAccount> actual = sut.accountInfo(ImmutableList.of(classicAddress));

		Assertions.assertEquals(expected, actual);
	}

	private FseAccount accountInfo() {
		return Mockito.mock(FseAccount.class);
	}

	@Test
	public void testPayment() {
		
		FsePaymentResult expected = fsepaymentResult();
		
		FsePaymentRequest paymentRequest = Mockito.mock(FsePaymentRequest.class);
		Mockito.when(xrplService.sendFsePayment(paymentRequest)).thenReturn(expected);

		FsePaymentResult actual = sut.payment(paymentRequest);

		Assertions.assertEquals(expected, actual);
	}

	private FsePaymentResult fsepaymentResult() {
		return Mockito.mock(FsePaymentResult.class);
	}

	@Test
	public void testPaymentTrustlines() {

		AirdropSummary expected = AirdropSummary.builder().build();
		FsePaymentTrustlinesRequest paymentRequest = Mockito.mock(FsePaymentTrustlinesRequest.class);

		Mockito.when(xrplService.sendFsePaymentToTrustlines(paymentRequest))
				.thenReturn(expected);

		AirdropSummary actual = sut.paymentTrustlines(paymentRequest);
		
		Assertions.assertEquals(expected, actual);
	}
}