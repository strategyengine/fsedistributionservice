package com.strategyengine.xrpl.fsedistributionservice.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AnalysisService {

	Set<String> getPaidAddresses(String classicAddress);

	Map<String, List<String>> getActivations(String issuingAddress, String currencyName, int minActivations);

}
