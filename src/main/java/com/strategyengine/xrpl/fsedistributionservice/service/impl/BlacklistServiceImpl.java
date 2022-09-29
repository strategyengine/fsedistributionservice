package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.GlobalIdClient;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.ScamAccountsClient;
import com.strategyengine.xrpl.fsedistributionservice.entity.ScammerAddressEnt;
import com.strategyengine.xrpl.fsedistributionservice.model.ScammerAddress;
import com.strategyengine.xrpl.fsedistributionservice.repo.ScammerAddressRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.BlacklistService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class BlacklistServiceImpl implements BlacklistService {

	private Set<String> blackListedCurrency = null;

	@Autowired
	protected ScamAccountsClient scamAccountsClient;

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
		
		checkForNewScammers();

	}
	
	//run every day at 4
	@Scheduled(cron="0 0 4 * * *")
	public void checkForNewScammers() {
		log.info("Startup or scheduled cron kickoff at 4am to load new blacklist");	
		List<ScammerAddress> scammers = scamAccountsClient.getScammers(0);
		int count = 0;
		while (!scammers.isEmpty()) {
			Date now = new Date();
			try {
				scammers.stream()
						.map(s -> ScammerAddressEnt.builder().createDate(now).updateDate(now)
								.type(s.getType()).address(s.getAccount()).build())
						.forEach(s -> scammerAddressRepo.save(s));
			} catch (Exception e) {
				// UNIQUE INDEX EXCEPTION IS EXPECTED. That's when we know we have them all
				// since the API returns the most recent first
				log.info("Blacklist load from api complete " + e.getMessage());
				break;
			}

			count++;
			scammers = scamAccountsClient.getScammers(count);
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
