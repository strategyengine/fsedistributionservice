package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.GlobalIdClient;
import com.strategyengine.xrpl.fsedistributionservice.entity.ScammerAddressEnt;
import com.strategyengine.xrpl.fsedistributionservice.repo.ScammerAddressRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.BlacklistService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class BlacklistServiceImpl implements BlacklistService {

	private Set<String> blackListedCurrency = null;

	@Autowired
	protected ScammerAddressRepo scammerAddressRepo;

	@VisibleForTesting
	@Autowired
	protected ObjectMapper objectMapper;
	
	@VisibleForTesting
	@Autowired
	protected GlobalIdClient globalIdClient;
	

	@PostConstruct
	public void init() throws Exception {
		
		try (InputStream inputStream = getClass().getResourceAsStream("/blacklisted_currency.txt");
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			blackListedCurrency = reader.lines().filter(s -> !StringUtils.isEmpty(s) && !s.startsWith("#")).collect(Collectors.toSet());
		}
		
	}
	

	@Override
	public Set<String> getBlackListedCurrencies() {

		return blackListedCurrency;
	}

	@Override
	public boolean isBlackListedAddress(String address) {

		if(globalIdClient.getGlobalIdXrpAddresses().stream().anyMatch(g-> g.getAddresses().contains(address))) {
			return false;
		}
		return scammerAddressRepo.count(Example.of(ScammerAddressEnt.builder().address(address).build())) > 0;
	}

}
