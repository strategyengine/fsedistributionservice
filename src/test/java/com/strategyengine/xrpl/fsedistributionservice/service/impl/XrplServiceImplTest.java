package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;
import org.xrpl.xrpl4j.model.client.accounts.TrustLine;
import org.xrpl.xrpl4j.model.ledger.AccountRootObject;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
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
	}

	@Test
	public void testGetTrustLines() throws Exception {

		Mockito.when(xrplClientService.getTrustLines(classicAddress)).thenReturn(accountLinesResult());
		List<FseTrustLine> actual = sut.getTrustLines(classicAddress);

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

		AccountRootObject accountRootObject = Mockito.mock(AccountRootObject.class);
		AccountInfoResult accountInfoResult = Mockito.mock(AccountInfoResult.class);

		Mockito.when(accountInfoResult.accountData()).thenReturn(accountRootObject);
		Mockito.when(accountRootObject.account()).thenReturn(Address.of(classicAddress));
		Mockito.when(accountRootObject.balance())
				.thenReturn(XrpCurrencyAmount.builder().value(UnsignedLong.valueOf(balance)).build());

		Mockito.when(xrplClientService.getAccountInfo(classicAddress)).thenReturn(accountInfoResult);

		FseAccount actual = sut.getAccountInfo(classicAddress);

		Assertions.assertEquals(
				FseAccount.builder().balance(new BigDecimal("0.000002")).classicAddress(classicAddress).build(),
				actual);
	}

	@Test
	public void testSendFsePayment() throws Exception {

		String message = "winner winner chicken dinner";

		FsePaymentRequest payment = fsepayment();

		Mockito.when(xrplClientService.sendFSEPayment(payment)).thenReturn(ImmutableList.of(message));

		FsePaymentResult actual = sut.sendFsePayment(payment);

		Assertions.assertEquals(FsePaymentResult.builder().responseMessages(ImmutableList.of(message)).build(), actual);

	}

	private FsePaymentRequest fsepayment() {

		String amount = "2";
		String fromPrivateKey = "shhhh";
		String signingKey = "ED123";
		String issuerAddress = "trusty";
		String currencyName = "FSE";
		return FsePaymentRequest.builder().fromSigningPublicKey(signingKey)
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

		FsePaymentRequest payment = fsepayment();
		FseTrustLine fseTrustLine = FseTrustLine.builder().balance(balance).currency(currency).classicAddress(toAddress).build();
		
		Mockito.when(xrplClientService.getTrustLines(issuerAddress)).thenReturn(accountLinesResult());

		Mockito.when(xrplClientService.sendFSEPayment(payment)).thenReturn(ImmutableList.of(message));
		
		Mockito.when(currencyHexService.isAcceptedCurrency(fseTrustLine, currencyName)).thenReturn(true);

		AccountInfoResult account =  Mockito.mock(AccountInfoResult.class);
		AccountRootObject aro = Mockito.mock(AccountRootObject.class);
		Mockito.when(aro.balance()).thenReturn(XrpCurrencyAmount.of(UnsignedLong.ONE));
		Mockito.when(aro.account()).thenReturn(Address.of(classicAddress));
		
		Mockito.when(account.accountData()).thenReturn(aro);
		Mockito.when(xrplClientService.getAccountInfo(classicAddress)).thenReturn(account);
		
		FsePaymentTrustlinesRequest request = FsePaymentTrustlinesRequest.builder().fromPrivateKey(fromPrivateKey)
				.trustlineIssuerClassicAddress(issuerAddress).currencyName(currencyName)
				.fromSigningPublicKey(signingKey).fromClassicAddress(classicAddress).amount(amount).build();

		List<FsePaymentResult> actual = sut.sendFsePaymentToTrustlines(request);

		Assertions.assertEquals(
				ImmutableList.of(FsePaymentResult.builder().responseMessages(ImmutableList.of(message)).build()),
				actual);

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
		FseTrustLine fseTrustLine = FseTrustLine.builder().balance(balance).currency(currency).classicAddress(toAddress).build();
		
		Mockito.when(xrplClientService.getTrustLines(issuerAddress)).thenReturn(accountLinesResult());

		Mockito.when(xrplClientService.sendFSEPayment(payment)).thenReturn(ImmutableList.of(message));
		
		Mockito.when(currencyHexService.isAcceptedCurrency(fseTrustLine, currencyName)).thenReturn(true);
		
		Mockito.when(transactionHistoryService.getPreviouslyPaidAddresses(payment.getFromClassicAddress(), payment.getCurrencyName(), payment.getTrustlineIssuerClassicAddress()))
		.thenReturn(Sets.newHashSet(payment.getToClassicAddresses()));

		AccountInfoResult account =  Mockito.mock(AccountInfoResult.class);
		AccountRootObject aro = Mockito.mock(AccountRootObject.class);
		Mockito.when(aro.balance()).thenReturn(XrpCurrencyAmount.of(UnsignedLong.ONE));
		Mockito.when(aro.account()).thenReturn(Address.of(classicAddress));
		
		Mockito.when(account.accountData()).thenReturn(aro);
		Mockito.when(xrplClientService.getAccountInfo(classicAddress)).thenReturn(account);
		
		FsePaymentTrustlinesRequest request = FsePaymentTrustlinesRequest.builder().fromPrivateKey(fromPrivateKey)
				.trustlineIssuerClassicAddress(issuerAddress).currencyName(currencyName).zeroBalanceOnly(true)
				.fromSigningPublicKey(signingKey).fromClassicAddress(classicAddress).amount(amount).build();

		List<FsePaymentResult> actual = sut.sendFsePaymentToTrustlines(request);

		Assertions.assertTrue(
				actual.isEmpty());

	}


}
