package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsTransactionResult;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.client.common.LedgerIndexBound;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.ImmutableIssuedCurrencyAmount;
import org.xrpl.xrpl4j.model.transactions.ImmutablePayment;
import org.xrpl.xrpl4j.model.transactions.Transaction;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;

public class TransactionHisotryServiceImplTest {

	@Mock
	private XrplClientService xrplClientService;

	private TransactionHistoryServiceImpl sut;

	private String fromAddress = "rFROMoHT7PMJxVyz3s8394zStRR68PqCz";
	private String balance = "2";
	private String currency = "FSE";
	private String toAddress = "rTOxQoHT7PMJxVyz3s8394zStRR68PqC2";
	private String issuer = "rISSUEHT7PMJxVyz3s8394zStRR68PqCz";

	private String qualityOut = "4";
	private String qualityIn = "3";
	private String limit = "2";
	private String limitPeer = "1";
	private String payAmount = ".589";
	private String transactionType = "PAYMENT";
	private UnsignedLong closeDateMillisFromRippleEpoch = UnsignedLong.valueOf(12345);
	private Date closeDate = new Date(946697145000l);

	private AccountTransactionsResult r;

	private AccountTransactionsTransactionResult<Transaction> xrpTransaction;

	private ImmutablePayment transaction;
	private LedgerIndex ledgerIndex = LedgerIndex.of("123");
	private LedgerIndexBound ledgerIndexBound = LedgerIndexBound.of(456l);

	@BeforeEach
	public void setup() throws Exception {
		MockitoAnnotations.openMocks(this);
		sut = new TransactionHistoryServiceImpl();
		sut.xrplClientService = xrplClientService;

		r = Mockito.mock(AccountTransactionsResult.class);
		xrpTransaction = Mockito.mock(AccountTransactionsTransactionResult.class);
		transaction = ImmutablePayment.builder().account(Address.of(fromAddress))
				.closeDate(closeDateMillisFromRippleEpoch).fee(XrpCurrencyAmount.ofDrops(1l))
				.sequence(UnsignedInteger.ONE).destination(Address.of(toAddress)).ledgerIndex(ledgerIndex)
				.account(Address.of(fromAddress)).amount(ImmutableIssuedCurrencyAmount.builder().currency(currency)
						.issuer(Address.of(issuer)).value(payAmount).build())
				.build();

	}

	@Test
	public void testGetTrustLines() throws Exception {
		Mockito.when(xrplClientService.getTransactions(fromAddress, Optional.empty())).thenReturn(r);
		Mockito.when(r.transactions()).thenReturn(ImmutableList.of(xrpTransaction));
		Mockito.when(xrpTransaction.transaction()).thenReturn(transaction);

		Mockito.when(xrplClientService.getTransactions(fromAddress, Optional.of(ledgerIndex)))
				.thenReturn(AccountTransactionsResult.builder().account(Address.of(fromAddress))
						.ledgerIndexMaximum(ledgerIndexBound).ledgerIndexMinimum(ledgerIndexBound)
						.limit(UnsignedInteger.ONE).build());

		List<FseTransaction> actual = sut.getTransactions(fromAddress, Optional.empty(), 10, Optional.empty());

		FseTransaction expected = FseTransaction.builder().amount(new BigDecimal(payAmount)).currency(currency)
				.fromAddress(fromAddress).issuerAddress(issuer).ledgerIndex(ledgerIndex.unsignedLongValue().longValue())
				.toAddress(toAddress).transactionType(transactionType).transactionDate(closeDate).build();

	//	Assertions.assertEquals(ImmutableList.of(expected), actual);
	}

}
