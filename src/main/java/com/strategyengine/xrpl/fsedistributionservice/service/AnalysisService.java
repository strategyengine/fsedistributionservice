package com.strategyengine.xrpl.fsedistributionservice.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.strategyengine.xrpl.fsedistributionservice.model.DropRecipientTransactions;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;

public interface AnalysisService {

	Set<String> getPaidAddresses(String classicAddress);

	Map<String, List<String>> getActivations(String issuingAddress, String currencyName, int minActivations);

	List<DropRecipientTransactions> getTransactionsForPaymentRequestId(Long dropRequestId);

	Map<String, List<FseTrustLine>> getVerifiedForIssuer(String issueAddress, String currency);

}
