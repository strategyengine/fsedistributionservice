package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.GlobalIdClient;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientServiceImpl;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRequestStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropType;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.PaymentType;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseSort;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.model.UserAddresses;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropSummaryService;
import com.strategyengine.xrpl.fsedistributionservice.service.BlacklistService;
import com.strategyengine.xrpl.fsedistributionservice.service.ConfigService;
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
	private BlacklistService blacklistService;
	
	@Mock
	private GlobalIdClient globalIdClient;
	
	@Mock
	private TransactionHistoryService transactionHistoryService;

	@Mock
	private AirdropSummaryService airdropSummaryService;
	
	@Mock
	private PaymentRequestRepo paymentRequestRepo;
	
	@Mock
	private ConfigService configService;
	
	private Date now;

	private XrplServiceImpl sut;

	private String classicAddress = "bingo!";
	private String balance = "2";
	private String currency = "FSE";
	String toAddress = "rJHxQoHT7PMJxVyz3s8394zStRR68PqCz";

	private String qualityOut = "4";
	private String qualityIn = "3";
	private String limit = "2";
	private String limitPeer = "1";

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		sut = new XrplServiceImpl() {
			@Override
			public Date now() {return now;}
		};
		sut.configService = configService;
		sut.xrplClientService = xrplClientService;
		sut.currencyHexService = currencyHexService;
		sut.validationService = validationService;
		sut.transactionHistoryService = transactionHistoryService;
		sut.blacklistService = blacklistService;
		sut.airdropSummaryService = airdropSummaryService;
		sut.globalIdClient = globalIdClient;
		sut.paymentRequestRepo = paymentRequestRepo;
		
		now = new Date();
	}

	@Test
	public void testCalcPayAmount() {
		
		String payAmt = sut.calculateRecipientPayAmount("62521958.4", ".0208", PaymentType.PROPORTIONAL);
		
		Assertions.assertEquals("1300456.7347", payAmt);
	}
	@Test
	public void testGetTrustLines() throws Exception {

		Mockito.when(xrplClientService.getTrustLines(classicAddress)).thenReturn(accountLinesResult());
		List<FseTrustLine> actual = sut.getTrustLines(classicAddress, FseSort.OLD);

		List<FseTrustLine> expected = ImmutableList
				.of(FseTrustLine.builder().balance(balance).currency(currency).classicAddress(toAddress).limit("1").build());

		Assertions.assertEquals(expected, actual);

	}

	private AccountLinesResult accountLinesResult() {

		Address add = Address.of(toAddress);
		return AccountLinesResult.builder()
				.addLines(TrustLine.builder().account(add)
						.qualityOut(UnsignedInteger.valueOf(qualityOut)).qualityIn(UnsignedInteger.valueOf(qualityIn))
						.limitPeer(limitPeer).limit(limit).balance(balance).currency(currency).build())
				.account(Address.builder().value(toAddress).build()).build();
	}
	
	@Test
	public void testFilterMinBalanceNoMinMax() {		
		Assertions.assertTrue(sut.matchesMinMax(fsePaymentTrustlinesRequest(), FseTrustLine.builder().build()));
	}
	@Test
	public void testFilterMinBalanceMatchMinNoMaxHCATS() {		
		Assertions.assertTrue(sut.matchesMinMax(fsePaymentTrustlinesRequest().toBuilder().minBalance(0.25).build(), FseTrustLine.builder().balance("2.120").build()));
	}
	@Test
	public void testFilterMinBalanceMatchMinNoMax() {		
		Assertions.assertTrue(sut.matchesMinMax(fsePaymentTrustlinesRequest().toBuilder().minBalance(.12).build(), FseTrustLine.builder().balance("0.120").build()));
	}
	@Test
	public void testFilterMinBalanceMaxMinMatchMax() {		
		Assertions.assertTrue(sut.matchesMinMax(fsePaymentTrustlinesRequest().toBuilder().maxBalance(2.4).minBalance(.12).build(), FseTrustLine.builder().balance("2.40").build()));
	}
	@Test
	public void testFilterMinBalanceBelowMinMatchMax() {		
		Assertions.assertFalse(sut.matchesMinMax(fsePaymentTrustlinesRequest().toBuilder().maxBalance(2.402).minBalance(2.401).build(), FseTrustLine.builder().balance("2.40").build()));
	}
	@Test
	public void testFilterMinBalanceNoMinAboveMax() {		
		Assertions.assertFalse(sut.matchesMinMax(fsePaymentTrustlinesRequest().toBuilder().maxBalance(2.402).build(), FseTrustLine.builder().balance("2.4021").build()));
	}
	@Test
	public void testFilterMinBalanceNoMinBelowMax() {		
		Assertions.assertTrue(sut.matchesMinMax(fsePaymentTrustlinesRequest().toBuilder().maxBalance(2.402).build(), FseTrustLine.builder().balance("2.40").build()));
	}	
	private FsePaymentTrustlinesRequest fsePaymentTrustlinesRequest() {
		return FsePaymentTrustlinesRequest.builder().fromPrivateKey("123")
				.agreeFee(true).trustlineIssuerClassicAddress("234").currencyName("BBB")
				.fromSigningPublicKey("ED1").fromClassicAddress(classicAddress).amount("1.0").build();
	}

	@Test
	public void testGetAccountInfo() throws Exception {

		FseAccount expected = FseAccount.builder().xrpBalance(new BigDecimal("0.000002")).classicAddress(classicAddress)
				.build();
		Mockito.when(xrplClientService.getAccountInfo(classicAddress)).thenReturn(expected);

		FseAccount actual = sut.getAccountInfo(ImmutableList.of(classicAddress), true).get(0);

		Assertions.assertEquals(expected, actual);
	}

	@Test
	public void testGlobalIdFilterSpecific() {
		
		Mockito.when(globalIdClient.getGlobalIdXrpAddresses()).thenReturn(ImmutableList.of(
				UserAddresses.builder().addresses(ImmutableList.of("1")).build(),
				UserAddresses.builder().addresses(ImmutableList.of("2", "3")).build(),
				UserAddresses.builder().addresses(ImmutableList.of("4","5")).build(),
				UserAddresses.builder().addresses(ImmutableList.of("6")).build()));
		
		//should only return 2 valid recipients since one user has both addresses 2,3
		Set<DropRecipientEnt> actual = sut.globalIdFilterSpecificAdds(Sets.newHashSet(
				DropRecipientEnt.builder().address("1").build(),
				DropRecipientEnt.builder().address("2").build(),
				DropRecipientEnt.builder().address("3").build()), fsepayment().toBuilder().globalIdVerified(true).build());

		Set<DropRecipientEnt> expected = Sets.newHashSet(
				DropRecipientEnt.builder().address("1").build(),
				DropRecipientEnt.builder().address("2").build());
	
		Assertions.assertEquals(expected, actual);

	}
	
	@Test
	public void testGlobalIdFilterSpecificFalse() {
		
		//should only return 2 valid recipients since one user has both addresses 2,3
		Set<DropRecipientEnt> actual = sut.globalIdFilterSpecificAdds(Sets.newHashSet(
				DropRecipientEnt.builder().address("1").build(),
				DropRecipientEnt.builder().address("2").build(),
				DropRecipientEnt.builder().address("3").build()), fsepayment().toBuilder().globalIdVerified(false).build());

		Set<DropRecipientEnt> expected = Sets.newHashSet(
				DropRecipientEnt.builder().address("1").build(),
				DropRecipientEnt.builder().address("2").build(),
				DropRecipientEnt.builder().address("3").build());
	
		Assertions.assertEquals(expected, actual);

	}
	
	
	@Test
	public void testGlobalIdFilterTrustlines() {
		
		Mockito.when(globalIdClient.getGlobalIdXrpAddresses()).thenReturn(ImmutableList.of(
				UserAddresses.builder().addresses(ImmutableList.of("1")).build(),
				UserAddresses.builder().addresses(ImmutableList.of("2", "3")).build(),
				UserAddresses.builder().addresses(ImmutableList.of("4","5")).build(),
				UserAddresses.builder().addresses(ImmutableList.of("6")).build()));
		
		//should only return 2 valid recipients since one user has both addresses 2,3
		List<FseTrustLine> actual = sut.globalIdFilterTrustlines(ImmutableList.of(
				FseTrustLine.builder().classicAddress("1").build(),
				FseTrustLine.builder().classicAddress("2").build(),
				FseTrustLine.builder().classicAddress("3").build()), fsetrustlinePayment().toBuilder().globalIdVerified(true).build());

		Assertions.assertEquals(2, actual.size());
		Assertions.assertEquals("1", actual.get(0).getClassicAddress());
		Assertions.assertEquals("2", actual.get(1).getClassicAddress());

	}
	
	@Test
	public void testGlobalIdFilterTrustlinesFalse() {
		
		
		//should only return 2 valid recipients since one user has both addresses 2,3
		List<FseTrustLine> actual = sut.globalIdFilterTrustlines(ImmutableList.of(
				FseTrustLine.builder().classicAddress("1").build(),
				FseTrustLine.builder().classicAddress("2").build(),
				FseTrustLine.builder().classicAddress("3").build()), fsetrustlinePayment().toBuilder().globalIdVerified(false).build());
		
		Assertions.assertEquals(3, actual.size());
		Assertions.assertEquals("1", actual.get(0).getClassicAddress());
		Assertions.assertEquals("2", actual.get(1).getClassicAddress());
		Assertions.assertEquals("3", actual.get(2).getClassicAddress());

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
	
	private FsePaymentTrustlinesRequest fsetrustlinePayment() {
		return FsePaymentTrustlinesRequest.builder()
				.amount(".1")
				.currencyName("FSE")
				.fromClassicAddress("777")
				.fromPrivateKey("ED1")
				.fromSigningPublicKey("ED2").trustlineIssuerClassicAddress("issuer").build();
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

	//	Mockito.when(xrplClientService.getTrustLines(issuerAddress)).thenReturn(accountLinesResult());

//		Mockito.when(xrplClientService.sendFSEPayment(payment, true)).thenReturn(ImmutableList.of(FsePaymentResult.builder().reason(message).build()));

		Mockito.when(transactionHistoryService.getPreviouslyPaidAddresses(payment.getFromClassicAddress(),
				 payment.getTrustlineIssuerClassicAddress()))
				.thenReturn(Sets.newHashSet(payment.getToClassicAddresses()));

//		FsePaymentResults expected = FsePaymentResults.builder().results(ImmutableList.of(FsePaymentResult.builder().build())).build();

		FseAccount fseAccount = FseAccount.builder().xrpBalance(new BigDecimal("0.000002"))
				.classicAddress(classicAddress).build();
		Mockito.when(xrplClientService.getAccountInfo(classicAddress)).thenReturn(fseAccount);
		Mockito.when(configService.isAirdropEnabled()).thenReturn(true);

		Mockito.when(currencyHexService.fixCurrencyCode(currencyName)).thenReturn(currencyName);
		FsePaymentTrustlinesRequest request = FsePaymentTrustlinesRequest.builder().fromPrivateKey(fromPrivateKey)
				.trustlineIssuerClassicAddress(issuerAddress).currencyName(currencyName).newTrustlinesOnly(true)
				.agreeFee(true).fromSigningPublicKey(signingKey).fromClassicAddress(classicAddress).amount(amount)
				.build();

		PaymentRequestEnt paymentRequestEnt = PaymentRequestEnt.builder()
				.memo(request.getMemo()).minBalance(
				request.getMinBalance() != null ? String.valueOf(request.getMinBalance()) : null)
				.maxBalance(
						request.getMaxBalance() != null ? String.valueOf(request.getMaxBalance())
								: null)
				.environment(null).amount(amount).createDate(now)
				.currencyName("FSE")
				.currencyNameForProcess("FSE")
				.paymentType(PaymentType.FLAT)
				.maxXrpFeePerTransaction(XrplClientServiceImpl.MAX_XRP_FEE_PER_TRANSACTION)
				.dropType(request.isGlobalIdVerified() ? DropType.GLOBALID : DropType.TRUSTLINE)
				.fromClassicAddress(request.getFromClassicAddress().trim()).fromPrivateKey(request.getFromPrivateKey().trim())
				.fromSigningPublicKey(request.getFromSigningPublicKey().trim()).maximumTrustlines(request.getMaximumTrustlines())
				.newTrustlinesOnly(request.isNewTrustlinesOnly()).status(DropRequestStatus.POPULATING_ADDRESSES)
				.trustlineIssuerClassicAddress(request.getTrustlineIssuerClassicAddress().trim()).updateDate(now)
				.build();
		
		Mockito.when(paymentRequestRepo.save(paymentRequestEnt)).thenReturn(paymentRequestEnt);
		
		String expectedError = "No eligible addresses found for issuingAddress trusty currency FSE newOnly:true";
		try {
			sut.sendFsePaymentToTrustlines(request, null);
		}catch(BadRequestException e) {
			Assertions.assertEquals(expectedError, e.getMessage());
		}
		

		//Mockito.verify(paymentRequestRepo).save(paymentRequestEnt.toBuilder().status(DropRequestStatus.QUEUED).build());

	}

}
