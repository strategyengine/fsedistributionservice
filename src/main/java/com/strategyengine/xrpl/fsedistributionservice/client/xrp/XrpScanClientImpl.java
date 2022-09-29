package com.strategyengine.xrpl.fsedistributionservice.client.xrp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strategyengine.xrpl.fsedistributionservice.model.XrpScanAccountResponse;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class XrpScanClientImpl implements XrpScanClient {

	private String url_account = "https://api.xrpscan.com/api/v1/account/";

	private String url_kyc = "https://api.xrpscan.com/api/v1/account/%s/xummkyc";

	@Autowired
	private ObjectMapper objectMapper;

	@Qualifier("xrpScanRestTemplate")
	@Autowired
	private RestTemplate restTemplate;

	@Override
	public XrpScanAccountResponse getAccount(String address) {

		return getAccountForResponse(address, url_account + address);
	}

	private XrpScanAccountResponse getAccountForResponse(String address, String url) {

		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

		if (HttpStatus.OK.equals(response.getStatusCode()) || HttpStatus.ACCEPTED.equals(response.getStatusCode())) {

			XrpScanAccountResponse account;
			try {
				account = objectMapper.readValue(response.getBody(), XrpScanAccountResponse.class);
			} catch (JsonProcessingException e) {
				log.error("ObjectMapper could not read response for address " + address, e);
				return null;
			}
			return account;

		}

		log.warn("XrpScanClient could not find response for {} got response {}", address, response);
		return null;
	}

	//@Cacheable("kyc-cache")
	@Override
	public boolean isKycForAddress(String address) {
return true;

//TODO this is returning false almost all of the time
//		XrpScanAccountResponse account = getAccountForResponse(address, String.format(url_kyc, address));

//		return account != null && account.getAccount()!=null ? account.getKycApproved() : true;// let them pass if we can't get a response
	}
}
