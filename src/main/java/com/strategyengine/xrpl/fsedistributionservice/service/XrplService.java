package com.strategyengine.xrpl.fsedistributionservice.service;

import java.util.List;
import java.util.Optional;

import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResults;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;

public interface XrplService {


	List<FseTrustLine> getTrustLines(String classicAddress, boolean sortByRich);
	
	List<FseTrustLine> getTrustLines(String classicAddress, Optional<String> currency, boolean includes, boolean sortByRich);

	List<FseAccount> getAccountInfo(List<String> classicAddress);

	List<FsePaymentResult> sendFsePayment(FsePaymentRequest paymentRequest);

	FsePaymentResults sendFsePaymentToTrustlines(FsePaymentTrustlinesRequest paymentRequest);


}
