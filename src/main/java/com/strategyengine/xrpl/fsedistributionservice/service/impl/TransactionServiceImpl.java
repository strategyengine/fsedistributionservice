package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.model.client.transactions.TransactionResult;
import org.xrpl.xrpl4j.model.transactions.Payment;

import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.entity.TransactionEnt;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.repo.TransactionRepo;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class TransactionServiceImpl implements TransactionService {

	@Autowired
	protected TransactionRepo transactionRepo;

	@Autowired
	protected XrplClientService xrplClientService;

	@Override
	public List<FseTransaction> getTransactionsForDropRecipientId(Long dropRecipientId) {

		List<TransactionEnt> transactions = transactionRepo
				.findAll(Example.of(TransactionEnt.builder().dropRecipientId(dropRecipientId).build()));

		return transactions.stream().map(t -> convert(t)).collect(Collectors.toList());
	}

	@Override
	public List<TransactionResult<Payment>> getXrplTransactionsByHashes(List<TransactionEnt> txHashes) {
		return txHashes.stream().map(t -> fetchTransactionFromLedger(t.getHash())).filter(t -> t!=null).collect(Collectors.toList());
	}

	private TransactionResult<Payment> fetchTransactionFromLedger(String hash) {
		try {
			return xrplClientService.getTransactionByHash(hash);
		}catch(JsonRpcClientErrorException je) {
			if(!je.getMessage().contains("not found")) {
				log.error("JsonRpcClientErrorException " + hash, je);
			}
			return null;
		}catch(Exception e) {
			log.error("Failed to fetch transaction from ledger based on hash " + hash, e);
			throw new RuntimeException("Failed to fetch transaction from ledger based on hash " + hash, e);
		}
	}

	private FseTransaction convert(TransactionEnt t) {
		return FseTransaction.builder().resultCode(t.getCode()).transactionHash(t.getHash()).reason(t.getFailReason())
				.transactionDate(t.getCreateDate()).build();
	}

}
