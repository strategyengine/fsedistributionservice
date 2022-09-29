package com.strategyengine.xrpl.fsedistributionservice.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;

public interface TransactionHistoryService {

	List<FseTransaction> getTransactions(String classicAddress, Optional<Long> maxLedgerIndex, int limit, Optional<Date> oldest);

	Set<String> getPreviouslyPaidAddresses(String classicAddress, String issuingAddress);

	List<FseTransaction> getTransactionsBetweenDates(String classicAddress, Date startTime, Date stopTime);

	List<FseTransaction> getTransactionBurns(String classicAddress);
	
}
