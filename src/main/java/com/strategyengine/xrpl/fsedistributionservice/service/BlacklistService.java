package com.strategyengine.xrpl.fsedistributionservice.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BlacklistService {

	Set<String> getBlackListedAddresses();

	Map<String, List<String>> getBlackListedFromAnalysis();

}
