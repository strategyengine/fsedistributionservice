package com.strategyengine.xrpl.fsedistributionservice.client.xrp;

import java.util.List;
import java.util.Optional;

import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsResult;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;

import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;

public interface XrplClientService {
	
	public static final String MAX_XRP_FEE_PER_TRANSACTION = ".0002";

	FseAccount getAccountInfo(String classicAddress) throws Exception;

	AccountLinesResult getTrustLines(String classicAddress) throws Exception;

	List<FsePaymentResult> sendFSEPayment(FsePaymentRequest paymentRequest) throws Exception;

	AccountTransactionsResult getTransactions(String classicAddress, Optional<LedgerIndex> maxLedger) throws Exception;

	String getActivatingAddress(String classicAddress) throws Exception;

}


