package com.strategyengine.xrpl.fsedistributionservice.service;

public interface ConfigService {

	boolean isAirdropEnabled();

	double getDouble(String key);

	int getInt(String key);

	String retryHungDropsEnvironment();

	String getSomeSecSaltySauce();

}
