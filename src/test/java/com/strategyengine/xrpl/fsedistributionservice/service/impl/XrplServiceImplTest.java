package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;
import org.xrpl.xrpl4j.model.client.accounts.TrustLine;
import org.xrpl.xrpl4j.model.transactions.Address;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedInteger;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.model.AirdropSummary;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResults;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropSummaryService;
import com.strategyengine.xrpl.fsedistributionservice.service.CurrencyHexService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;
import com.strategyengine.xrpl.fsedistributionservice.service.ValidationService;

public class XrplServiceImplTest {

	@Mock
	private XrplClientService xrplClientService;

	@Mock
	private CurrencyHexService currencyHexService;

	@Mock
	private ValidationService validationService;

	@Mock
	private TransactionHistoryService transactionHistoryService;

	@Mock
	private AirdropSummaryService airdropSummaryService;

	private XrplServiceImpl sut;

	private String classicAddress = "bingo!";
	private String balance = "2";
	private String currency = "FSE";
	String toAddress = "happy birthday";

	private String qualityOut = "4";
	private String qualityIn = "3";
	private String limit = "2";
	private String limitPeer = "1";

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		sut = new XrplServiceImpl();
		sut.xrplClientService = xrplClientService;
		sut.currencyHexService = currencyHexService;
		sut.validationService = validationService;
		sut.transactionHistoryService = transactionHistoryService;
		sut.airdropSummaryService = airdropSummaryService;
	}

	@Test
	public void testGetTrustLines() throws Exception {

		Mockito.when(xrplClientService.getTrustLines(classicAddress)).thenReturn(accountLinesResult());
		List<FseTrustLine> actual = sut.getTrustLines(classicAddress, true);

		List<FseTrustLine> expected = ImmutableList
				.of(FseTrustLine.builder().balance(balance).currency(currency).classicAddress(toAddress).build());

		Assertions.assertEquals(expected, actual);

	}

	private AccountLinesResult accountLinesResult() {

		return AccountLinesResult.builder()
				.addLines(TrustLine.builder().account(Address.of(toAddress))
						.qualityOut(UnsignedInteger.valueOf(qualityOut)).qualityIn(UnsignedInteger.valueOf(qualityIn))
						.limitPeer(limitPeer).limit(limit).balance(balance).currency(currency).build())
				.account(Address.builder().value(toAddress).build()).build();
	}

	@Test
	public void testGetAccountInfo() throws Exception {

		FseAccount expected = FseAccount.builder().xrpBalance(new BigDecimal("0.000002")).classicAddress(classicAddress)
				.build();
		Mockito.when(xrplClientService.getAccountInfo(classicAddress)).thenReturn(expected);

		FseAccount actual = sut.getAccountInfo(ImmutableList.of(classicAddress)).get(0);

		Assertions.assertEquals(expected, actual);
	}

	@Test
	public void testSendFsePayment() throws Exception {

		String message = "winner winner chicken dinner";

		FsePaymentRequest payment = fsepayment();

		Mockito.when(xrplClientService.sendFSEPayment(payment)).thenReturn(ImmutableList.of(FsePaymentResult.builder().reason(message).build()));

		List<FsePaymentResult> actual = sut.sendFsePayment(payment);

		Assertions.assertEquals(FsePaymentResult.builder().reason(message).build(), actual.get(0));

	}

	private FsePaymentRequest fsepayment() {

		String amount = "2";
		String fromPrivateKey = "shhhh";
		String signingKey = "ED123";
		String issuerAddress = "trusty";
		String currencyName = "FSE";
		return FsePaymentRequest.builder().fromSigningPublicKey(signingKey).agreeFee(true)
				.toClassicAddresses(ImmutableList.of(toAddress)).fromPrivateKey(fromPrivateKey)
				.fromClassicAddress(classicAddress).trustlineIssuerClassicAddress(issuerAddress)
				.currencyName(currencyName).amount(amount).build();
	}

	@Test
	public void testSendFsePaymentToTrustlines() throws Exception {

		String issuerAddress = "trusty";
		String currencyName = "FSE";
		String amount = "2";
		String message = "lucky dog!";
		String signingKey = "ED123";
		String fromPrivateKey = "shhhh";

		AirdropSummary expected = AirdropSummary.builder().build();
		FsePaymentRequest payment = fsepayment();
		FseTrustLine fseTrustLine = FseTrustLine.builder().balance(balance).currency(currency).classicAddress(toAddress)
				.build();

		Mockito.when(xrplClientService.getTrustLines(issuerAddress)).thenReturn(accountLinesResult());

		Mockito.when(xrplClientService.sendFSEPayment(payment)).thenReturn(ImmutableList.of(FsePaymentResult.builder().reason(message).build()));

		Mockito.when(currencyHexService.isAcceptedCurrency(fseTrustLine, currencyName)).thenReturn(true);

		FseAccount fseAccount = FseAccount.builder().xrpBalance(new BigDecimal("0.000002"))
				.classicAddress(classicAddress).build();
		Mockito.when(xrplClientService.getAccountInfo(classicAddress)).thenReturn(fseAccount);

		Mockito.when(airdropSummaryService.createSummary(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyList(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(expected);

		FsePaymentTrustlinesRequest request = FsePaymentTrustlinesRequest.builder().fromPrivateKey(fromPrivateKey)
				.agreeFee(true).trustlineIssuerClassicAddress(issuerAddress).currencyName(currencyName)
				.fromSigningPublicKey(signingKey).fromClassicAddress(classicAddress).amount(amount).build();

		FsePaymentResults actual = sut.sendFsePaymentToTrustlines(request);

		Assertions.assertEquals(message, actual.getResults().get(0).getReason());

	}

	@Test
	public void testSendFsePaymentToTrustlinesZeroBalanceAlreadyPaid() throws Exception {

		String issuerAddress = "trusty";
		String currencyName = "FSE";
		String amount = "2";
		String message = "lucky dog!";
		String signingKey = "ED123";
		String fromPrivateKey = "shhhh";

		FsePaymentRequest payment = fsepayment();
		FseTrustLine fseTrustLine = FseTrustLine.builder().balance(balance).currency(currency).classicAddress(toAddress)
				.build();

		Mockito.when(xrplClientService.getTrustLines(issuerAddress)).thenReturn(accountLinesResult());

		Mockito.when(xrplClientService.sendFSEPayment(payment)).thenReturn(ImmutableList.of(FsePaymentResult.builder().reason(message).build()));

		Mockito.when(currencyHexService.isAcceptedCurrency(fseTrustLine, currencyName)).thenReturn(true);

		Mockito.when(transactionHistoryService.getPreviouslyPaidAddresses(payment.getFromClassicAddress(),
				payment.getCurrencyName(), payment.getTrustlineIssuerClassicAddress()))
				.thenReturn(Sets.newHashSet(payment.getToClassicAddresses()));

		FsePaymentResults expected = FsePaymentResults.builder().results(ImmutableList.of(FsePaymentResult.builder().build())).build();

		FseAccount fseAccount = FseAccount.builder().xrpBalance(new BigDecimal("0.000002"))
				.classicAddress(classicAddress).build();
		Mockito.when(xrplClientService.getAccountInfo(classicAddress)).thenReturn(fseAccount);

		FsePaymentTrustlinesRequest request = FsePaymentTrustlinesRequest.builder().fromPrivateKey(fromPrivateKey)
				.trustlineIssuerClassicAddress(issuerAddress).currencyName(currencyName).newTrustlinesOnly(true)
				.agreeFee(true).fromSigningPublicKey(signingKey).fromClassicAddress(classicAddress).amount(amount)
				.build();

		FsePaymentResults actual = sut.sendFsePaymentToTrustlines(request);

		expected.getResults().get(0).setReason(actual.getResults().get(0).getReason());
		Assertions.assertEquals(expected, actual);

	}

}
