package com.strategyengine.xrpl.fsedistributionservice.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AirdropResultsParser {

	@Test
	public void outputFails() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		StringBuilder sb = new StringBuilder();

		try (InputStream inputStream = getClass().getResourceAsStream("/fse_october_results.json");
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			reader.lines().forEach(l -> sb.append(l));
		}

//		FsePaymentResults results = objectMapper.readValue(sb.toString(), FsePaymentResults.class);

//		List<String> addresses = results.getResults().stream()
//				.filter(a -> "telINSUF_FEE_P".equals(a.getResponseCode()))
//						.map(a -> a.getClassicAddress()).collect(Collectors.toList());
		
//		System.out.println(objectMapper.writeValueAsString(addresses));
	}

}
