package com.strategyengine.xrpl.fsedistributionservice.service;

import com.strategyengine.xrpl.fsedistributionservice.model.PaymentsChange;

public interface PaymentsChangeService {

	void updatePaymentAmounts(PaymentsChange paymentsChange);

	void removeRecipient(Long dropRecipId, String privateKey);

}
