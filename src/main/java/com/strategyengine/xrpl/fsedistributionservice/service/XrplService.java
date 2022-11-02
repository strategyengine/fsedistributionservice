package com.strategyengine.xrpl.fsedistributionservice.service;

import java.util.List;
import java.util.Optional;

import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseSort;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;

public interface XrplService {

	
	List<FseTrustLine> getTrustLines(String classicAddress, Optional<String> currency, Optional<String> currencyForProcess, boolean includes, FseSort sort);

	List<FseAccount> getAccountInfo(List<String> classicAddress, boolean includeTrust);

	PaymentRequestEnt sendFsePayment(FsePaymentRequest paymentRequest);

	PaymentRequestEnt sendFsePaymentToTrustlines(FsePaymentTrustlinesRequest paymentRequest, List<DropRecipientEnt> retryFailedAddresses);

	FsePaymentRequest cancelJob(String privateKey, String issuingAddress);

	List<FseTrustLine> getTrustLines(String classicAddress, FseSort sort);

	void approveAirdrop(Long paymentRequestId, String privKey);

	List<FseTrustLine> fetchAllTrustlines(FsePaymentTrustlinesRequest paymentRequestPre);


}
