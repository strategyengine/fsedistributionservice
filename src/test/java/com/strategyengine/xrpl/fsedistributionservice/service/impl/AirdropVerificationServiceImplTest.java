package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableList;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropType;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.ConfigService;
import com.strategyengine.xrpl.fsedistributionservice.service.PaymentService;

public class AirdropVerificationServiceImplTest {

	private AirdropVerificationServiceImpl sut;

	@Mock
	private PaymentRequestRepo paymentRequestRepo;

	@Mock
	private PaymentService paymentService;

	@Mock
	private ConfigService configService;

	private Date now = new Date();

	@Captor
	ArgumentCaptor<DropRecipientEnt> dropRecipientEntCaptor;

	@Captor
	ArgumentCaptor<FsePaymentRequest> paymentRequestCaptor;

	@BeforeEach
	public void setup() throws Exception {

		MockitoAnnotations.openMocks(this);
		sut = new AirdropVerificationServiceImpl() {

			@Override
			public Date now() {
				return now;
			}
		};
		sut.paymentRequestRepo = paymentRequestRepo;
		sut.paymentService = paymentService;
		sut.configService = configService;

		Mockito.when(configService.getDouble(AirdropVerificationServiceImpl.DEFAULT_SERVICE_FEE_PER_INTERVAL_VERFIED))
				.thenReturn(.1);
		Mockito.when(configService.getDouble(AirdropVerificationServiceImpl.DEFAULT_SERVICE_FEE_PER_INTERVAL_UNVERFIED))
				.thenReturn(1.0);
		Mockito.when(configService.getInt(AirdropVerificationServiceImpl.SERVICE_FEE_INTERVAL_VERIFIED))
				.thenReturn(5000);
		Mockito.when(configService.getInt(AirdropVerificationServiceImpl.SERVICE_FEE_INTERVAL_UNVERIFIED))
				.thenReturn(25000);

	}

	@Test
	public void testGetTransactionMap() {

		String issuer = "rJWRqdKTRfqoeHERBDJukWWNTRrnwayDca";
		String toAddress = "rppPTyQ9jpoYAf7FjPejSkj1mogiDCxRqn";
		List<FseTransaction> transactions = ImmutableList.of(FseTransaction.builder().toAddress(toAddress)
				.issuerAddress(issuer).amount(new BigDecimal("0.1")).build());
		Map<String, List<FseTransaction>> m = sut.getTransactionMap(
				PaymentRequestEnt.builder().amount("0.1000").trustlineIssuerClassicAddress(issuer).build(),
				transactions);

		Assertions.assertEquals(transactions, m.get(toAddress));

	}
	
	@Test
	public void testIsCrossSnapshotDropFalse() {
		
		
		Assertions.assertFalse(sut.isCrossSnapshotDrop(PaymentRequestEnt.builder()
				.currencyName("A").trustlineIssuerClassicAddress("r1")
				.snapshotCurrencyName("A").snapshotTrustlineIssuerClassicAddress("r1").build()));
	}
	
	@Test
	public void testIsCrossSnapshotDropTrue() {
		
		Assertions.assertTrue(sut.isCrossSnapshotDrop(PaymentRequestEnt.builder()
				.currencyName("A").trustlineIssuerClassicAddress("r1")
				.snapshotCurrencyName("B").snapshotTrustlineIssuerClassicAddress("r2").build()));

		Assertions.assertTrue(sut.isCrossSnapshotDrop(PaymentRequestEnt.builder()
				.currencyName("A").trustlineIssuerClassicAddress("r1")
				.snapshotCurrencyName("B").snapshotTrustlineIssuerClassicAddress("r1").build()));
		
		Assertions.assertTrue(sut.isCrossSnapshotDrop(PaymentRequestEnt.builder()
				.currencyName("A").trustlineIssuerClassicAddress("r1")
				.snapshotCurrencyName("A").snapshotTrustlineIssuerClassicAddress("r2").build()));
				
	}

	@Test
	public void testAmountsEqual() {

		Assertions.assertTrue(sut.amountsEqual(new BigDecimal("0.1"), "0.1000"));
		Assertions.assertTrue(sut.amountsEqual(new BigDecimal("10.12"), "10.1200"));
		Assertions.assertTrue(sut.amountsEqual(new BigDecimal("120.1200"), "120.120"));
		Assertions.assertTrue(sut.amountsEqual(new BigDecimal(".1"), "0.1000"));
		Assertions.assertTrue(sut.amountsEqual(new BigDecimal("0.10"), ".1000"));
		Assertions.assertTrue(sut.amountsEqual(new BigDecimal("0.10000100000000000000"), "0.1000010000"));

	}

	@Test
	public void testFees() {

		valid("0", "0.50", "1.0", 1, DropType.SPECIFICADDRESSES);

	}

	@Test
	public void testFeesGlobalid() {

		valid("0", "0.05", "0.1", 1, DropType.GLOBALID);
	}

	@Test
	public void testFeesGlobalId_specific() {

		valid("0", "0.05", "0.1", 1, DropType.GLOBALID_SPECIFICADDRESSES);
	}

	@Test
	public void testFeesTrustlines() {

		valid("0", "0.50", "1.0", 1, DropType.TRUSTLINE);
	}

	@Test
	public void testFees3() {

		valid("0", "0.50", "1.0", 6200, DropType.SPECIFICADDRESSES);

	}

	@Test
	public void testFees4() {

		valid("0.12", "0.44", "1.0", 8000, DropType.SPECIFICADDRESSES);

	}

	@Test
	public void testFees4Globalid() {

		valid("0.12", "0.04", "0.2", 8000, DropType.GLOBALID);

	}

	@Test
	public void testFees4GlobalSpecific() {

		valid("0.12", "0.04", "0.2", 8000, DropType.GLOBALID_SPECIFICADDRESSES);

	}

	@Test
	public void testFees4Trustlines() {

		valid("0.12", "0.44", "1.0", 8000, DropType.TRUSTLINE);

	}

	@Test
	public void testFees6() {

		valid("0.01", "1.495", "3.0", 52550, DropType.SPECIFICADDRESSES);

	}

	@Test
	public void testFees7() {

		valid("0.0", "1.50", "3.0", 52550, DropType.SPECIFICADDRESSES);

	}

	@Test
	public void testFees8() {

		valid("1.01", "0.995", "3.0", 52550, DropType.SPECIFICADDRESSES);

	}

	private void valid(String paidBefore, String toPayAndBurn, String totalPaid, int size, DropType dropType) {
		PaymentRequestEnt p = PaymentRequestEnt.builder().id(2000000l).fromClassicAddress("r")
				.fromSigningPublicKey("1234").trustlineIssuerClassicAddress("1234").dropType(dropType)
				.currencyName("te").amount("1").fromPrivateKey("234").feesPaid(paidBefore)
				.build();

		BigDecimal amountToPay = new BigDecimal(toPayAndBurn).multiply(new BigDecimal("2"));
		BigDecimal burnFee = amountToPay.multiply(new BigDecimal(".9")).stripTrailingZeros();
		BigDecimal ardyFee = amountToPay.multiply(new BigDecimal(".1")).stripTrailingZeros();

		sut.collectFees(p, size);

		Mockito.verify(paymentService, Mockito.times(2)).allowPaymentNoPersist(paymentRequestCaptor.capture(),dropRecipientEntCaptor.capture());	
		
		List<FsePaymentRequest> requests = paymentRequestCaptor.getAllValues();
		List<DropRecipientEnt> dropRecipients = dropRecipientEntCaptor.getAllValues();
		
		FsePaymentRequest expectedArdyPayReq = FsePaymentRequest.builder().amount(ardyFee.toString())
				.trustlineIssuerClassicAddress(AirdropVerificationServiceImpl.FSE_ISSUING_ADDRESS).currencyName("FSE")
				.fromClassicAddress(p.getFromClassicAddress()).fromPrivateKey(p.getFromPrivateKey())
				.fromSigningPublicKey(p.getFromSigningPublicKey()).build();
		
		Assertions.assertEquals(expectedArdyPayReq,requests.get(0));

		DropRecipientEnt expectedArdyDropRecip =AirdropVerificationServiceImpl.SERVICE_FEE_ADDRESS.toBuilder().id(0l).dropRequestId(p.getId())
				.payAmount(ardyFee.toString()).createDate(now).updateDate(now).build(); 
		Assertions.assertEquals(expectedArdyDropRecip ,dropRecipients.get(0));
		
		Assertions.assertEquals(FsePaymentRequest.builder().amount(burnFee.toString())
				.trustlineIssuerClassicAddress(AirdropVerificationServiceImpl.FSE_ISSUING_ADDRESS).currencyName("FSE")
				.fromClassicAddress(p.getFromClassicAddress()).fromPrivateKey(p.getFromPrivateKey())
				.fromSigningPublicKey(p.getFromSigningPublicKey()).build(),requests.get(1));

		Assertions.assertEquals(AirdropVerificationServiceImpl.BURN_ISSUER_FSE_ADDRESS.toBuilder().id(0l).dropRequestId(p.getId())
				.payAmount(burnFee.toString()).createDate(now).updateDate(now).build(),dropRecipients.get(1));

		Mockito.verify(paymentRequestRepo).save(p.toBuilder().feesPaid(totalPaid).build());
	}

	// overpaid for some reason, don't charge again
	@Test
	public void testFeesOverpaid() {

		PaymentRequestEnt p = PaymentRequestEnt.builder().id(1000000l).fromClassicAddress("r").feesPaid("1.1")
				.fromSigningPublicKey("1234").trustlineIssuerClassicAddress("1234").currencyName("te").amount("1")
				.fromPrivateKey("234").build();

		sut.collectFees(p, 2000);

		Mockito.verifyNoInteractions(paymentService);

		Mockito.verify(paymentRequestRepo, Mockito.never()).save(Mockito.any());
	}

	@Test
	public void testFeesPaid() {

		PaymentRequestEnt p = PaymentRequestEnt.builder().id(1000000l).fromClassicAddress("r").feesPaid("1")
				.fromSigningPublicKey("1234").trustlineIssuerClassicAddress("1234").dropType(DropType.TRUSTLINE)
				.currencyName("te").amount("1").fromPrivateKey("234").build();

		sut.collectFees(p, 43);

		Mockito.verifyNoInteractions(paymentService);

		Mockito.verify(paymentRequestRepo, Mockito.never()).save(Mockito.any());
	}
	
	@Test
	public void testTotalFeesRequiredForSize() {
		
		BigDecimal val = sut.getTotalFeeRequiredForSize(657, DropType.SPECIFICADDRESSES, true);
		
		Assertions.assertEquals(new BigDecimal("2.00"),val);
		
	}

}
