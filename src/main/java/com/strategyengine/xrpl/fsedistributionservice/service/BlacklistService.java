package com.strategyengine.xrpl.fsedistributionservice.service;

import java.util.Set;

public interface BlacklistService {

	Set<String> getBlackListedCurrencies();

	boolean isBlackListedAddress(String address);

}
