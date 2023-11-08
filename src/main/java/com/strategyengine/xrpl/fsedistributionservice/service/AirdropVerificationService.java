package com.strategyengine.xrpl.fsedistributionservice.service;

import java.util.List;
import java.util.Map;

import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.TransactionEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.XrplNetwork;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;

public interface AirdropVerificationService {

	PaymentRequestEnt getPaymentRequestToProcess(String uuid);

	String lockPaymentRequest();

	void resetUuidForProcessing(PaymentRequestEnt paymentRequest);

	boolean didReceiveDrop(DropRecipientEnt recipient, Map<String, List<FseTransaction>> airdropByToAddress, List<TransactionEnt> transactionHashes);

	Map<String, List<FseTransaction>> getTransactionMap(PaymentRequestEnt paymentRequest,
			List<FseTransaction> transactions);

	PaymentRequestEnt verifyDropComplete(PaymentRequestEnt paymentRequestInitial, List<FseTransaction> transactions,
			boolean releaseLock, XrplNetwork xrplNetwork);

}
