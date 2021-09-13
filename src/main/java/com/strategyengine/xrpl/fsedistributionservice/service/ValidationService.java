package com.strategyengine.xrpl.fsedistributionservice.service;

import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;

public interface ValidationService {

	void validateClassicAddress(String classicAddress);

	void validate(FsePaymentRequest payment);

	void validate(FsePaymentTrustlinesRequest p);

}
