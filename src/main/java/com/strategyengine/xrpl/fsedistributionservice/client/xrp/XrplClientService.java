package com.strategyengine.xrpl.fsedistributionservice.client.xrp;

import java.util.List;
import java.util.Optional;

import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountTransactionsResult;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.client.transactions.TransactionResult;
import org.xrpl.xrpl4j.model.transactions.Payment;
import org.xrpl.xrpl4j.model.transactions.Transaction;

import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.XrplNetwork;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;

public interface XrplClientService {

	public static final String MAX_XRP_FEE_PER_TRANSACTION = ".0002";
	
	FseAccount getAccountInfo(String classicAddress, XrplNetwork xrplNetwork) throws Exception;

	AccountLinesResult getTrustLines(String classicAddress, XrplNetwork xrplNetwork) throws Exception;

	DropRecipientEnt sendFSEPayment(FsePaymentRequest paymentRequest, DropRecipientEnt recipientAddress, XrplNetwork xrplNetwork) throws Exception;

	AccountTransactionsResult getTransactions(String classicAddress, Optional<LedgerIndex> maxLedger, XrplNetwork xrplNetwork) throws Exception;

	String getActivatingAddress(String classicAddress, XrplNetwork xrplNetwork) throws Exception;

	void setTrust(String currencyCode, String addressWantingTrust, String issuingAddress, String trustValue,
			String publicKey, String privateKeyStr, XrplNetwork xrplNetwork);

	TransactionResult<Payment> getTransactionByHash(String hash, XrplNetwork xrplNetwork) throws JsonRpcClientErrorException;

	List<SubmitResult<Transaction>> cancelOpenOffers(String seed, XrplNetwork xrplNetwork) throws Exception;

	List<SubmitResult<Transaction>> cancelOpenOffers(String address, String pubKey, String privKey, XrplNetwork xrplNetwork) throws Exception;

}
