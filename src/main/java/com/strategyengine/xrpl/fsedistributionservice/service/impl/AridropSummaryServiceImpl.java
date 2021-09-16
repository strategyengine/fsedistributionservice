package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.model.AirdropSummary;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropSummaryService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class AridropSummaryServiceImpl implements AirdropSummaryService {

	@VisibleForTesting
	@Autowired
	protected XrplService xrplService;

	@VisibleForTesting
	@Autowired
	protected TransactionHistoryService transactionHistoryService;

	@Override
	public AirdropSummary airdropSummary(String classicAddress, String issuingAddress, String currency, Date startTime,
			Date stopTime, BigDecimal dropAmount) {

		List<FseTrustLine> trustLinesShouldHaveReceived = xrplService.getTrustLines(issuingAddress,
				Optional.of(currency), true);

		return createSummary(classicAddress, issuingAddress, currency, trustLinesShouldHaveReceived, startTime,
				stopTime, dropAmount);
	}

	@Override
	public AirdropSummary createSummary(@NonNull String classicAddress, @NonNull String issuingAddress,
			@NonNull String currency, List<FseTrustLine> trustLinesShouldHaveReceived, Date startTime, Date stopTime,
			@NonNull BigDecimal dropAmount) {
		try {

			List<FseTransaction> transactions = transactionHistoryService.getTransactionsBetweenDates(classicAddress,
					currency, startTime, stopTime);

			Map<String, List<FseTransaction>> airdDropByToAddress = new HashMap<String, List<FseTransaction>>();
			for (FseTransaction t : transactions) {
				if (t.getAmount() != null && t.getAmount().equals(dropAmount)
						&& issuingAddress.equals(t.getIssuerAddress()) && currency.equals(t.getCurrency())) {
					List<FseTransaction> transactionsForAddress = airdDropByToAddress.get(t.getToAddress());
					if (transactionsForAddress == null) {
						transactionsForAddress = new ArrayList<FseTransaction>();
					}
					transactionsForAddress.add(t);
					airdDropByToAddress.put(t.getToAddress(), transactionsForAddress);
				}
			}

			List<String> shouldHaveRecievedDropButDidNot = trustLinesShouldHaveReceived.stream()
					.filter(t -> !didReceiveDrop(t, airdDropByToAddress)).map(t -> t.getClassicAddress())
					.collect(Collectors.toList());

			int totalDrops = trustLinesShouldHaveReceived.size() - shouldHaveRecievedDropButDidNot.size();

			return AirdropSummary.builder().totalTrustlines(trustLinesShouldHaveReceived.size())
					.amountDistributed(dropAmount.multiply(new BigDecimal(totalDrops)))
					.totalAddressesReceivedDrop(totalDrops).totalTrustlines(trustLinesShouldHaveReceived.size())
					.classicAddressShouldHaveRecievedButDidNot(shouldHaveRecievedDropButDidNot).build();

		} catch (Exception e) {
			log.error("Error generating airdrop summary", e);
		}
		return null;
	}

	private boolean didReceiveDrop(FseTrustLine trustLine, Map<String, List<FseTransaction>> airdropByToAddress) {

		List<FseTransaction> transactions = airdropByToAddress.get(trustLine.getClassicAddress());

		return transactions != null && transactions.stream()
				.filter(t -> TransactionHistoryServiceImpl.TRANSACTION_TYPE_PAYMENT.equals(t.getTransactionType()))
				.findAny().isPresent();
	}

}
