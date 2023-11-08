package com.strategyengine.xrpl.fsedistributionservice.service;

import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.XrplNetwork;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;

public interface PaymentService {


	DropRecipientEnt allowPayment(FsePaymentRequest paymentRequest, DropRecipientEnt recipient);

	DropRecipientEnt allowPaymentNoPersist(FsePaymentRequest paymentRequest, DropRecipientEnt dropRecipientEnt,
			XrplNetwork xrplNetwork);

}
