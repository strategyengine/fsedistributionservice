package com.strategyengine.xrpl.fsedistributionservice.client.xrp;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.strategyengine.xrpl.fsedistributionservice.model.XrplDataNftDto;

import lombok.extern.log4j.Log4j2;


/**
 * This needs to be retried { "responseCode": "telINSUF_FEE_P", "reason": "Fee
 * insufficient.", "classicAddress": "rwWiGVvBCeuiyL5QhKumxuAwDU9Ytffaf2" }
 * 
 * @author barry
 *
 */
@Log4j2
@Service
public class XrplNftClientImpl implements XrplNftClient {

	@Value("${fsedistributionservice.xrplnft.url}")
	private String url;
	
	@Autowired
	private ObjectMapper objectMapper;

	@Qualifier("xrplNftRestTemplate")
	@Autowired
	private RestTemplate restTemplate;


	@Override
	public List<XrplDataNftDto> getNftOwnersForIssuer(String nftIssuer, Optional<Long> taxon) {

		if(nftIssuer == null || !nftIssuer.startsWith("r")) {
			return ImmutableList.of();
		}
		
		
		String urlWithIssuer = url + nftIssuer;
		
		if(taxon.isPresent()) {
			urlWithIssuer += "?taxon=" + taxon.get();
		}

		try {

			ResponseEntity<String> response = restTemplate.getForEntity(urlWithIssuer, String.class);

			if (HttpStatus.OK.equals(response.getStatusCode())
					|| HttpStatus.ACCEPTED.equals(response.getStatusCode())) {

				List<XrplDataNftDto> owners = objectMapper.readValue(response.getBody(),
						new TypeReference<List<XrplDataNftDto>>() {
						});

				return owners;
			}
			log.warn("XrplNftClient could not find response for {} got response {}", url, response);
		} catch (Exception e) {
			log.error("Unable to load nfts, will check next time " + url + " " + e.getMessage());
		}

		return ImmutableList.of();
	}

}
