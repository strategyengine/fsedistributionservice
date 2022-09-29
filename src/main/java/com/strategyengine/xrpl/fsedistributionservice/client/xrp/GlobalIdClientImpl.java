package com.strategyengine.xrpl.fsedistributionservice.client.xrp;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strategyengine.xrpl.fsedistributionservice.model.UserAddresses;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class GlobalIdClientImpl implements GlobalIdClient {


	//1012 total
	//724 3+ verified
	//98 2 verified
	//190 1 verified
	
	@Value("${fsedistributionservice.globalid.whitelist.url}")
	private String url;

	@Autowired
	private ObjectMapper objectMapper;

	@Qualifier("globalIsServiceRestTemplate")
	@Autowired
	private RestTemplate restTemplate;

	@Override
	@Cacheable("whitelistCache")
	public List<UserAddresses> getGlobalIdXrpAddresses() {


		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

		if (HttpStatus.OK.equals(response.getStatusCode()) || HttpStatus.ACCEPTED.equals(response.getStatusCode())) {

			List<UserAddresses> gids;
			try {
				gids = objectMapper.readValue(response.getBody(),  new TypeReference<List<UserAddresses>>(){});
			} catch (JsonProcessingException e) {
				log.error("ObjectMapper could not read response for xrp globalids ", e);
				return null;
			}
			return gids;

		}

		log.warn("xrp global id adds could not find response", response);
		return null;
	}


}
