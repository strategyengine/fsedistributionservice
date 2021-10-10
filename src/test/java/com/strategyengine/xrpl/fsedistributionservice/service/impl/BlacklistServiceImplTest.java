package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

//TODO fix this

public class BlacklistServiceImplTest {

	private BlacklistServiceImpl sut;

	@BeforeEach
	public void setup() throws Exception {

		sut = new BlacklistServiceImpl();
		sut.objectMapper = new ObjectMapper();

		sut.init();
	}

	@Test
	public void testGetAll() throws Exception{

		Map<String, List<String>> bl = sut.getBlackListedFromAnalysis();
		
		
		String largestKey = null;
		
		List<String> removeKeys = new ArrayList<>();
		
		for(String key : bl.keySet()) {
			if(largestKey ==null || bl.get(key).size() > bl.get(largestKey).size()) {
				largestKey = key;
			}
			
			if( bl.get(key).size() < 39) {
				removeKeys.add(key);
			}
		}
		removeKeys.stream().forEach(k -> bl.remove(k));
	
		String json = new ObjectMapper().writeValueAsString(bl);
		
		File f = new File("temp.json");
		f.createNewFile();
		FileWriter fw = new		FileWriter(f);
		
		fw.write(json);
		fw.close();
		System.out.println(json);
	}

//	@Test
	public void testGetSet() {

		Set<String> bl = sut.getBlackListedAddresses();

		Assertions.assertEquals(9793, bl.size());
	}

}
