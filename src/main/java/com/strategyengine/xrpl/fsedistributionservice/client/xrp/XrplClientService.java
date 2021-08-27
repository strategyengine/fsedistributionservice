package com.strategyengine.xrpl.fsedistributionservice.client.xrp;

import java.util.List;

import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;

import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;

public interface XrplClientService {

	AccountInfoResult getAccountInfo(String classicAddress) throws Exception;

	AccountLinesResult getTrustLines(String classicAddress) throws Exception;

	List<String> sendFSEPayment(FsePaymentRequest paymentRequest) throws Exception;

}


