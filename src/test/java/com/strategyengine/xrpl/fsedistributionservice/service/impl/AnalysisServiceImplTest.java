package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.service.BlacklistService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

public class AnalysisServiceImplTest {

	private AnalysisServiceImpl sut;

	@Mock
	private XrplService xrplService;

	@Mock
	private XrplClientService xrplClientService;

	@Mock
	private TransactionHistoryService transactionHistoryService;

	@Mock
	private BlacklistService blacklistService;
	
	private String classicAddress = "booyah!";

	private String childAddress = "phht!";
	@BeforeEach
	public void setup() throws Exception {

		MockitoAnnotations.openMocks(this);
		sut = new AnalysisServiceImpl();
		sut.objectMapper = new ObjectMapper();
		sut.xrplClientService = xrplClientService;
		sut.xrplService = sut.xrplService;
		sut.transactionHistoryService = transactionHistoryService;
		sut.blacklistService = blacklistService;

	}

	@Test
	public void testGetPaid() {
		
		
		Mockito.when(transactionHistoryService.getTransactions(classicAddress, Optional.empty(),
				20000)).thenReturn(ImmutableList.of(FseTransaction.builder().transactionType("PAYMENT").toAddress(childAddress).build()));

		Set<String> expected = Sets.newHashSet(childAddress);
		Set<String> actual = sut.getPaidAddresses(classicAddress);

		Assertions.assertEquals(expected, actual);
	}

	
	
}
