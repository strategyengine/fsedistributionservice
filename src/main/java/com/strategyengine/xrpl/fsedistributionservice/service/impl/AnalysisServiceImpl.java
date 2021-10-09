package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.service.AnalysisService;
import com.strategyengine.xrpl.fsedistributionservice.service.BlacklistService;
import com.strategyengine.xrpl.fsedistributionservice.service.TransactionHistoryService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class AnalysisServiceImpl implements AnalysisService {

	@VisibleForTesting
	@Autowired
	protected XrplService xrplService;

	@VisibleForTesting
	@Autowired
	protected XrplClientService xrplClientService;

	@VisibleForTesting
	@Autowired
	protected TransactionHistoryService transactionHistoryService;
	
	@VisibleForTesting
	@Autowired
	protected BlacklistService blacklistService;
	
	@VisibleForTesting
	@Autowired
	protected ObjectMapper objectMapper;

	@Override
	public Set<String> getPaidAddresses(String classicAddress) {
		List<FseTransaction> transactions = transactionHistoryService.getTransactions(classicAddress, Optional.empty(),
				20000);

		return transactions.stream().filter(t -> "PAYMENT".equals(t.getTransactionType())).map(t -> t.getToAddress())
				.collect(Collectors.toSet());
	}

	@Override
	public Map<String, List<String>> getActivations(String issuingAddress, String currencyName, int minActivations) {
		try {

			try {
				List<FseTrustLine> trustlinePool = xrplService.getTrustLines(issuingAddress, Optional.of(currencyName),
						true, false);

				if (trustlinePool == null) {
					throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
							String.format("No trustlines found for %s %s ", issuingAddress, currencyName));
				}
				Map<String, List<String>> parentPool = new HashMap<>();
				int count = 0;
				for (FseTrustLine trustline : trustlinePool) {
					try {

						count++;

						if (blacklistService.getBlackListedAddresses().contains(trustline.getClassicAddress())) {
							// this address is already blacklisted
							continue;
						}

						String activatingAddress = xrplClientService
								.getActivatingAddress(trustline.getClassicAddress());

						if (activatingAddress == null) {
							continue;
						}
						List<String> activatedChildren = parentPool.get(activatingAddress);

						if (activatedChildren == null) {
							activatedChildren = new ArrayList<String>();
						}
						activatedChildren.add(trustline.getClassicAddress());

						if (trustlinePool.stream().filter(s -> s.getClassicAddress().equals(activatingAddress))
								.findAny().isEmpty()) {
							// activating address does not have the trustline, let's assume it is an
							// exchange address that activated many XRP addresses and skip it
							continue;
						}

						parentPool.put(activatingAddress, activatedChildren);
						log.info("count:{} IssuingAdd: {} Currency: {}  Parent: {} activated address: {}", count,
								issuingAddress, currencyName, activatingAddress, trustline.getClassicAddress());

					} catch (Exception e) {
						log.error("Error getActivations for trustline " + trustline, e);
					}
				}

				log.info("Parent Child activations: \n" + objectMapper.writeValueAsString(parentPool));

				// remove keys that do not meet the minimum number of activated children
				List<String> removeKeys = parentPool.keySet().stream()
						.filter(k -> parentPool.get(k).size() <= minActivations).collect(Collectors.toList());
				removeKeys.stream().forEach(k -> parentPool.remove(k));

				log.info("Parent Child activations with too many children! : \n"
						+ objectMapper.writeValueAsString(parentPool));

				return parentPool;
			} catch (Exception e) {
				log.error("Error getActivations " + issuingAddress, e);
			}
			return null;

		} catch (Exception e) {
			log.error("Error running analysis", e);

			throw new RuntimeException(e);
		}

	}

}
