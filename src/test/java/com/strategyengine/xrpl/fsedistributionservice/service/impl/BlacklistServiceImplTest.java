package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BlacklistServiceImplTest {

	private BlacklistServiceImpl sut;

	@BeforeEach
	public void setup() throws Exception {

		sut = new BlacklistServiceImpl();
		sut.objectMapper = new ObjectMapper();

		sut.init();
	}

	@Test
	public void testGetAll() {

		Map<String, List<String>> bl = sut.getBlackListedFromAnalysis();
		
		
		String largestKey = null;
		
		for(String key : bl.keySet()) {
			if(largestKey ==null || bl.get(key).size() > bl.get(largestKey).size()) {
				largestKey = key;
			}
		}
		
		Assertions.assertEquals("rMeAdAJyR42WzGnphfwSJfQGk6r7kvSWeB", largestKey);
		Assertions.assertEquals(527, bl.get(largestKey).size());
	}

	@Test
	public void testGetSet() {

		Set<String> bl = sut.getBlackListedAddresses();

		Assertions.assertEquals(9793, bl.size());
	}

}
