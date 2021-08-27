package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesMinTriggeredRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

public class TrustlineTriggerDropServiceTest {

	@Mock
	private XrplService xrplService;

	private TrustlineTriggerDropServiceImpl sut;

	@BeforeEach
	public void setup() {

		MockitoAnnotations.openMocks(this);
		sut = new TrustlineTriggerDropServiceImpl();
		sut.xrplService = xrplService;
	}

	@Test
	public void testTriggerAirdrop() {

		
		int minTrustLinesToTriggerDrop = 2;

		FsePaymentTrustlinesRequest trustlineReq = fseTrustLinepayment();

		FsePaymentTrustlinesMinTriggeredRequest triggeredReq = FsePaymentTrustlinesMinTriggeredRequest.builder()
				.minTrustLinesTriggerValue(minTrustLinesToTriggerDrop).trustlinePaymentRequest(trustlineReq).build();

		Mockito.when(xrplService.getTrustLines(trustlineReq.getTrustlineIssuerClassicAddress()))
				.thenReturn(trustLines(1));
		Mockito.when(xrplService.getTrustLines(trustlineReq.getTrustlineIssuerClassicAddress()))
				.thenReturn(trustLines(2));

		//will not trigger with first check.   Second check will only have 1 trustline but third check will have 2 trustlines and trigger the drop
		sut.triggerDrop(trustLines(1), triggeredReq, 100);

		
		Mockito.verify(xrplService).sendFsePaymentToTrustlines(trustlineReq);
	}

	private List<FseTrustLine> trustLines(int lines) {

		List<FseTrustLine> trustLines = new ArrayList<FseTrustLine>();
		for (int i = 0; i < lines; i++) {
			trustLines.add(FseTrustLine.builder().balance("5").currency("FSE")
					.classicAddress(UUID.randomUUID().toString()).build());
		}

		return trustLines;

	}

	private FsePaymentTrustlinesRequest fseTrustLinepayment() {

		String amount = "2";
		String fromPrivateKey = "shhhh";
		String signingKey = "ED123";
		String issuerAddress = "trusty";
		String currencyName = "FSE";
		String classicAddress = "classic";

		return FsePaymentTrustlinesRequest.builder().fromPrivateKey(fromPrivateKey)
				.trustlineIssuerClassicAddress(issuerAddress).currencyName(currencyName)
				.fromSigningPublicKey(signingKey).fromClassicAddress(classicAddress).amount(amount)
				.trustlineIssuerClassicAddress(issuerAddress).build();
				
	}

}
