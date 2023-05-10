package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import com.strategyengine.xrpl.fsedistributionservice.entity.ConfigEnt;
import com.strategyengine.xrpl.fsedistributionservice.repo.ConfigRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.ConfigService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class ConfigServiceImpl implements ConfigService {

	
	@Autowired
	private ConfigRepo configRepo;

	@Override
	public String getSomeSecSaltySauce() {
		Optional<ConfigEnt> config = configRepo.findOne(Example.of(ConfigEnt.builder().key("SEC.SALTY.SAUCE").build()));

		return config.isEmpty() ? "BLAH" : config.get().getValue(); 

	}
	
	
	@Override
	public boolean isAirdropEnabled() {
		Optional<ConfigEnt> config = configRepo.findOne(Example.of(ConfigEnt.builder().key("AIRDROP.ENABLED").build()));

		if (config.isPresent() && !Boolean.valueOf(config.get().getValue())) {
			return false;
		}

		return true;

	}

	@Override
	public double getDouble(String key) {
		Optional<ConfigEnt> config = configRepo.findOne(Example.of(ConfigEnt.builder().key(key).build()));
		
		return Double.parseDouble(config.get().getValue());
	}
	
	@Override
	public int getInt(String key) {
		Optional<ConfigEnt> config = configRepo.findOne(Example.of(ConfigEnt.builder().key(key).build()));
		
		return Integer.parseInt(config.get().getValue());
	}

	@Override
	public String retryHungDropsEnvironment() {
		Optional<ConfigEnt> config = configRepo.findOne(Example.of(ConfigEnt.builder().key("RETRY.HUNG.DROP.ENV").build()));
		
		return config.get().getValue();
	}


	@Override
	public String getScamAcountKey() {
		
		Optional<ConfigEnt> config = configRepo.findOne(Example.of(ConfigEnt.builder().key("SCAM.ACCOUNT.KEY").build()));
		if(config.isEmpty()) {
			return null;
		}
		return config.get().getValue();
		
	}


	@Override
	public String getAutoBurnSeed() {
		Optional<ConfigEnt> config = configRepo.findOne(Example.of(ConfigEnt.builder().key("AUTOBURN.SEED").build()));

		return config.isEmpty() ? "BLAH" : config.get().getValue(); 
	}
}
