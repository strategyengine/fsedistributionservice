package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.GlobalIdClient;
import com.strategyengine.xrpl.fsedistributionservice.entity.ScammerAddressEnt;
import com.strategyengine.xrpl.fsedistributionservice.model.UserAddresses;
import com.strategyengine.xrpl.fsedistributionservice.repo.ScammerAddressRepo;

//TODO fix this

public class BlacklistServiceImplTest {

	private BlacklistServiceImpl sut;
	
	@Mock
	private GlobalIdClient globalIdClient; 
	@Mock
	private ScammerAddressRepo scammerAddressRepo;
	

	@BeforeEach
	public void setup() throws Exception {
		MockitoAnnotations.openMocks(this);
		sut = new BlacklistServiceImpl();
		sut.objectMapper = new ObjectMapper();
		sut.globalIdClient = globalIdClient;
		sut.scammerAddressRepo = scammerAddressRepo;

	}

	@Test
	public void testBlacklistedNotWhitelisted() {

		String address = "9";
	
		Mockito.when(scammerAddressRepo.count(Example.of(ScammerAddressEnt.builder().address(address).build()))).thenReturn(Long.valueOf(1l));
		
		mockWhiteList();
		Assertions.assertTrue(sut.isBlackListedAddress(address));
		
	}
	
	

	@Test
	public void testBlacklistedAndWhiteListed() {

		String address = "3";
	
		mockWhiteList();
		Assertions.assertFalse(sut.isBlackListedAddress(address));
		
		Mockito.verifyZeroInteractions(scammerAddressRepo);
		
	}

	@Test
	public void testNeither() {

		String address = "22";
		Mockito.when(scammerAddressRepo.count(Example.of(ScammerAddressEnt.builder().address(address).build()))).thenReturn(Long.valueOf(0l));
		mockWhiteList();
		Assertions.assertFalse(sut.isBlackListedAddress(address));
		
	}
	
	private void mockWhiteList() {
		Mockito.when(globalIdClient.getGlobalIdXrpAddresses()).thenReturn(ImmutableList.of(
				UserAddresses.builder().addresses(ImmutableList.of("1")).build(),
				UserAddresses.builder().addresses(ImmutableList.of("2", "3")).build(),
				UserAddresses.builder().addresses(ImmutableList.of("4","5")).build(),
				UserAddresses.builder().addresses(ImmutableList.of("6")).build()));
		
	}

}
