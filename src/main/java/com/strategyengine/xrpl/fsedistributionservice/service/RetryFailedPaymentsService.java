package com.strategyengine.xrpl.fsedistributionservice.service;

import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.RetryPaymentRequest;

public interface RetryFailedPaymentsService {

	FsePaymentResult retryFailedPayments(RetryPaymentRequest retryPaymentRequest);
	

}
