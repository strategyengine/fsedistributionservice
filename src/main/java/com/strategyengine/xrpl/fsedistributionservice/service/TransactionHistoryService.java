package com.strategyengine.xrpl.fsedistributionservice.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;

public interface TransactionHistoryService {

	List<FseTransaction> getTransactions(String classicAddress, Optional<Long> maxLedgerIndex, int limit);

	Set<String> getPreviouslyPaidAddresses(String classicAddress, String currency, String issuingAddress);
	
}
