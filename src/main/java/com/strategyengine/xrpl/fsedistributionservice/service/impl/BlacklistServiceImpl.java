package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.strategyengine.xrpl.fsedistributionservice.service.BlacklistService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class BlacklistServiceImpl implements BlacklistService {

	private Set<String> blackListedAddresses = null;

	private Map<String, List<String>> blackListedFromAnalysis = new HashMap<>();

	@VisibleForTesting
	@Autowired
	protected ObjectMapper objectMapper;

	@PostConstruct
	public void init() throws Exception {

		try (InputStream inputStream = getClass().getResourceAsStream("/blacklisted.txt");
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			blackListedAddresses = reader.lines().filter(s -> !StringUtils.isEmpty(s) && !s.startsWith("#")).collect(Collectors.toSet());
		}

		try (InputStream inputStream = getClass().getResourceAsStream("/blacklist_analysis_fse.json");
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			StringBuilder sb = new StringBuilder();

			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}

			Map<String, List<String>> blackListedFromAnalysisFse = objectMapper.readValue(sb.toString(),
					new TypeReference<Map<String, List<String>>>() {
					});

			// even though they are scammers, have a heart, let at least their parent
			// address have the airdrop.
			// blackListedAddresses.addAll(blackListedFromAnalysis.keySet());
			blackListedFromAnalysisFse.keySet().stream()
					.forEach(k -> blackListedAddresses.addAll(blackListedFromAnalysisFse.get(k)));
			
			blackListedFromAnalysis.putAll(blackListedFromAnalysisFse);

		}

		try (InputStream inputStream = getClass().getResourceAsStream("/blacklist_analysis_sse.json");
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			StringBuilder sb = new StringBuilder();

			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}

			Map<String, List<String>> blackListedFromAnalysisSse = objectMapper.readValue(sb.toString(),
					new TypeReference<Map<String, List<String>>>() {
					});

			// even though they are scammers, have a heart, let at least their parent
			// address have the airdrop.
			// blackListedAddresses.addAll(blackListedFromAnalysis.keySet());
			blackListedFromAnalysisSse.keySet().stream()
					.forEach(k -> blackListedAddresses.addAll(blackListedFromAnalysisSse.get(k)));

			blackListedFromAnalysis.putAll(blackListedFromAnalysisSse);
		}
		
		blackListedFromAnalysis.putAll(ImmutableMap.of("scammers", blackListedAddresses.stream().collect(Collectors.toList())));

	}

	@Override
	public Set<String> getBlackListedAddresses() {

		return blackListedAddresses;
	}

	@Override
	public Map<String, List<String>> getBlackListedFromAnalysis() {

		return blackListedFromAnalysis;
	}
}
