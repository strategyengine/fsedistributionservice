package com.strategyengine.xrpl.fsedistributionservice.service;

import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;

public interface PaymentService {

	DropRecipientEnt allowPaymentNoPersist(FsePaymentRequest paymentRequest, DropRecipientEnt dropRecipientEnt);

	DropRecipientEnt allowPayment(FsePaymentRequest paymentRequest, DropRecipientEnt recipient);

}
