package com.strategyengine.xrpl.fsedistributionservice.service;

import java.util.List;

import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;

public interface XrplService {

	List<FseTrustLine> getTrustLines(String classicAddress);

	FseAccount getAccountInfo(String classicAddress);

	FsePaymentResult sendFsePayment(FsePaymentRequest paymentRequest);

	List<FsePaymentResult> sendFsePaymentToTrustlines(FsePaymentTrustlinesRequest paymentRequest);

}
