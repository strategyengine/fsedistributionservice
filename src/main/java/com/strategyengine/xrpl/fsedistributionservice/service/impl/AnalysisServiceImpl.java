package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.GlobalIdClient;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrpScanClient;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.model.DropRecipientTransactions;
import com.strategyengine.xrpl.fsedistributionservice.model.FseSort;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTransaction;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.model.UserAddresses;
import com.strategyengine.xrpl.fsedistributionservice.model.XrpScanAccountResponse;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropRecipientRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
import com.strategyengine.xrpl.fsedistributionservice.service.AirdropVerificationService;
import com.strategyengine.xrpl.fsedistributionservice.service.AnalysisService;
import com.strategyengine.xrpl.fsedistributionservice.service.BlacklistService;
import com.strategyengine.xrpl.fsedistributionservice.service.CurrencyHexService;
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
	protected XrpScanClient xrpScanClient;

	@VisibleForTesting
	@Autowired
	protected TransactionHistoryService transactionHistoryService;

	@VisibleForTesting
	@Autowired
	protected PaymentRequestRepo paymentRequestRepo;

	@VisibleForTesting
	@Autowired
	protected DropRecipientRepo dropRecipientRepo;

	@VisibleForTesting
	@Autowired
	protected BlacklistService blacklistService;

	@VisibleForTesting
	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected AirdropVerificationService airdropVerificationService;

	@VisibleForTesting
	@Autowired
	protected GlobalIdClient globalIdClient;

	@VisibleForTesting
	@Autowired
	protected CurrencyHexService currencyHexService;

	private int runningVerifiedAnalysis = 0;

	@Override
	public Set<String> getPaidAddresses(String classicAddress) {
		List<FseTransaction> transactions = transactionHistoryService.getTransactions(classicAddress, Optional.empty(),
				20000, Optional.empty());

		return transactions.stream().filter(t -> "PAYMENT".equals(t.getTransactionType())).map(t -> t.getToAddress())
				.collect(Collectors.toSet());
	}

	@Override
	public Map<String, List<String>> getActivations(String issuingAddress, String currencyName, int minActivations) {
		try {

			try {
				String currencyForProcess = currencyHexService.fixCurrencyCode(currencyName);
				
				List<FseTrustLine> trustlinePool = xrplService
						.getTrustLines(issuingAddress, Optional.of(currencyName), Optional.of(currencyForProcess), true, FseSort.YOUNG)
						.subList(0, 10000);

				if (trustlinePool == null) {
					throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
							String.format("No trustlines found for %s %s ", issuingAddress, currencyName));
				}
				Map<String, List<String>> parentPool = new HashMap<>();
				int count = 0;
				for (FseTrustLine trustline : trustlinePool) {
					try {

						count++;

						if (blacklistService.isBlackListedAddress(trustline.getClassicAddress())) {
							// this address is already blacklisted
							continue;
						}

						XrpScanAccountResponse account = xrpScanClient.getAccount(trustline.getClassicAddress());

						if (account == null) {
							continue;
						}
						List<String> activatedChildren = parentPool.get(account.getParent());

						if (activatedChildren == null) {
							activatedChildren = new ArrayList<String>();
						}
						activatedChildren.add(trustline.getClassicAddress());

						if (account.getParentName() != null || account.getAccountName() != null
								|| trustlinePool.stream().filter(s -> s.getClassicAddress().equals(account.getParent()))
										.findAny().isEmpty()) {
							log.info("Skipping account " + account);
							// activating address does not have the trustline, let's assume it is an
							// exchange address that activated many XRP addresses and skip it
							continue;
						}

						parentPool.put(account.getParent(), activatedChildren);
						log.info("count:{} IssuingAdd: {} Currency: {}  Parent: {} activated address: {}", count,
								issuingAddress, currencyName, account.getParent(), trustline.getClassicAddress());

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

	@Override
	public List<DropRecipientTransactions> getTransactionsForPaymentRequestId(Long dropRequestId) {

		PaymentRequestEnt paymentRequest = paymentRequestRepo.findById(dropRequestId).get();

		List<DropRecipientEnt> recipientsToValidate = dropRecipientRepo
				.findAll(Example.of(DropRecipientEnt.builder().dropRequestId(paymentRequest.getId()).build()));

//		recipientsToValidate.addAll(dropRecipientRepo.findAll(Example.of(DropRecipientEnt.builder()
//				.status(DropRecipientStatus.SENDING).dropRequestId(paymentRequest.getId()).build())));

		List<FseTransaction> transactions = transactionHistoryService.getTransactionsBetweenDates(
				paymentRequest.getFromClassicAddress(), paymentRequest.getCreateDate(), new Date());

		Map<String, List<FseTransaction>> transactionMap = airdropVerificationService.getTransactionMap(paymentRequest,
				transactions);

		List<String> keysWithMultTxs = transactionMap.keySet().stream().filter(k -> transactionMap.get(k).size() > 1)
				.collect(Collectors.toList());

		Map<String, List<FseTransaction>> transactionMapWithMultiple = airdropVerificationService.getTransactionMap(
				paymentRequest,
				transactions.stream().filter(s -> keysWithMultTxs.contains(s)).collect(Collectors.toList()));

		// TODO this is sending an empty list for transactions
		List<DropRecipientTransactions> txs = recipientsToValidate.stream()
				.map(d -> DropRecipientTransactions.builder().dropRecipient(d)
						.transactionsFromMap(transactionMap.get(d.getAddress()))
						.didReceive(airdropVerificationService.didReceiveDrop(d, transactionMap, ImmutableList.of()))
						.transactions(transactions(d, transactions)).build())
				.collect(Collectors.toList());

		// is it all just blacklisted addresses
		txs.stream().filter(s -> !s.isDidReceive()).forEach(s -> log.info(s));

		txs.stream().filter(s -> s.isDidReceive()).forEach(s -> log.info(s));

		return txs;
	}

	private List<FseTransaction> transactions(DropRecipientEnt d, List<FseTransaction> transactions) {
		return transactions.stream().filter(t -> d.getAddress().equals(t.getToAddress())).collect(Collectors.toList());
	}

	@Cacheable("verifiedCache")
	@Override
	public Map<String, List<FseTrustLine>> getVerifiedForIssuer(String issueAddress, String currency) {

		checkRateLimit();

		try {
			runningVerifiedAnalysis++;
			String currencyFixed = currencyHexService.fixCurrencyCode(currency);
			List<FseTrustLine> trustlinesAll = xrplService.getTrustLines(issueAddress, Optional.of(currency), Optional.of(currencyFixed), true,
					FseSort.RICH);

			List<UserAddresses> verifiedGlobalId = globalIdClient.getGlobalIdXrpAddresses();

			List<FseTrustLine> verifiedTls = trustlinesAll.stream()
					.filter(t -> contains(verifiedGlobalId, t.getClassicAddress())).collect(Collectors.toList());

			List<FseTrustLine> trustlinesUnverified = trustlinesAll.stream().filter(t -> !verifiedTls.contains(t))
					.collect(Collectors.toList());

			Map<String, List<FseTrustLine>> tls = new HashMap<String, List<FseTrustLine>>();

			tls.put("VERIFIED", verifiedTls);
			tls.put("UNVERIFIED", trustlinesUnverified);

			return tls;
		} finally {
			runningVerifiedAnalysis--;
		}
	}

	private void checkRateLimit() {

		if (runningVerifiedAnalysis > 2) {
			runningVerifiedAnalysis = 2;
		}

		if (runningVerifiedAnalysis >= 2) {
			log.info("Will not run verified analysis check.  Already running " + runningVerifiedAnalysis);
			throw new BadRequestException(
					"This analysis has strict rate limiting due to the XRPL search.  It is currently in use by another user please try back later.");
		}

	}

	private boolean contains(List<UserAddresses> verified, String classicAddress) {
		if (StringUtils.isEmpty(classicAddress)) {
			return false;
		}
		return verified.stream().anyMatch(v -> v.getAddresses().contains(classicAddress));

	}

}
