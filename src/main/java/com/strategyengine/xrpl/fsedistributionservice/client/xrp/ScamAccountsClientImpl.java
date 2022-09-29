package com.strategyengine.xrpl.fsedistributionservice.client.xrp;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.strategyengine.xrpl.fsedistributionservice.model.ScammerAddress;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class ScamAccountsClientImpl implements ScamAccountsClient {

	private String url_scammers = "https://api.xrplorer.com/custom/airdropfarmingaccounts/";

	private String key = "680817af-faf6-4f11-b3bc-ba87217553c2";

	@Autowired
	private ObjectMapper objectMapper;

	@Qualifier("scammerLookupRestTemplate")
	@Autowired
	private RestTemplate restTemplate;

	/**
	 * API: https://api.xrplorer.com/custom/airdropfarmingaccounts
	 * 
	 * Params: per_page (1-5000), default 1000 page (int) Count and paging
	 * information in headers: Link, X-Total-Count, Pagination-Count,
	 * Pagination-Page, Pagination-Limit
	 * 
	 * The results are always ordered by the order we have added them, so if you run
	 * it periodically, you can always just fetch from the end
	 * 
	 * Pass api key as header ' x-api-key' on your GET request. Key:
	 * 680817af-faf6-4f11-b3bc-ba87217553c2
	 * 
	 * response
	 * [{"account":"raXetXmmzVWdcnCrYzcoMMiWfr8urXJ47M","type":"shared_parent"},{"account":"rDH8n45NxeR1ZgRPcdnDMSjXeVxvv6Wd2f","type":"shared_parent"},{"account":"rJezhFuCKMw3cRxq6ceGqqXqtxHMVvCo9Q","type":"shared_parent"},{"account":"r35mVUXBJTiUFTZahEgMncCNEx69WsT72x","type":"shared_parent"},{"account":"rDE9Z7yx2dQgxNRmu3VWfDGBJ46iofL189","type":"shared_parent"}]
	 */

	@Cacheable("scamAccountsCache")
	@Override
	public List<ScammerAddress> getScammers(int page) {

		String url = url_scammers + "?per_page=100&page=" + page;

		try {
			MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
			headers.add("Content-Type", "application/json");
			headers.add("x-api-key", key);

			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
					new HttpEntity<Object>(headers), String.class);

			if (HttpStatus.OK.equals(response.getStatusCode())
					|| HttpStatus.ACCEPTED.equals(response.getStatusCode())) {

				List<ScammerAddress> scammers = objectMapper.readValue(response.getBody(),
						new TypeReference<List<ScammerAddress>>() {
						});

				return scammers;
			}
			log.warn("XrpScanClient could not find response for {} got response {}", url, response);
		} catch (Exception e) {
			log.error("Unable to load new scammers, will check next time " + url + " " + e.getMessage());
			return ImmutableList.of();
		}

		return null;
	}

}
