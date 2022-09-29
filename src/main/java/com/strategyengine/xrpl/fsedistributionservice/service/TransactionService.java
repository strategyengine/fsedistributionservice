package com.strategyengine.xrpl.fsedistributionservice.service;

import java.util.List;

import org.xrpl.xrpl4j.model.client.transactions.TransactionResult;
import org.xrpl.xrpl4j.model.transactions.Payment;

import com.strategyengine.xrpl.fsedistributionservice.entity.TransactionEnt;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;

public interface TransactionService {

	List<FseTransaction> getTransactionsForDropRecipientId(Long dropRecipientId);

	List<TransactionResult<Payment>> getXrplTransactionsByHashes(List<TransactionEnt> txHashes);

}
