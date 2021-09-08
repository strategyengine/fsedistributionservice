package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.xrpl.xrpl4j.codec.binary.types.CurrencyType;

import com.google.common.base.Strings;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;

public class CurrencyHexServiceImplTest {

	public static final char DOUBLE_QUOTE = '"';
	private final CurrencyType codec = new CurrencyType();

	private CurrencyHexServiceImpl sut;

	@BeforeEach
	public void setup() {

		MockitoAnnotations.openMocks(this);
		sut = new CurrencyHexServiceImpl();
	}

	@Test
	public void testSkipConvertTrustLineCurrencyIso() {
		
		String balance = "1.25";
		String classicAdd = "1234ADSF";
		
		FseTrustLine trustLine = FseTrustLine.builder().currency("FSE").balance(balance).classicAddress(classicAdd).build();
		
		FseTrustLine actual = sut.convertCurrencyHexToCode(trustLine);
		
		Assertions.assertEquals(trustLine, actual);
	}
	
	@Test
	public void testConvertTrustLineCurrencyHex() {
		
		String balance = "1.25";
		String classicAdd = "1234ADSF";
		
		FseTrustLine trustLine = FseTrustLine.builder().currency("4653450000000000000000000000000000000000").balance(balance).classicAddress(classicAdd).build();
		
		FseTrustLine actual = sut.convertCurrencyHexToCode(trustLine);

		FseTrustLine expected = FseTrustLine.builder().currency("FSE").balance(balance).classicAddress(classicAdd).build();
		
		
		Assertions.assertEquals(expected, actual);
	}
	
	@Test
	public void testCreateCurrencyTypeHex() throws Exception {

		String actual3CharMid = new String(Hex.decodeHex("0000000000000000000000004653450000000000")).trim();
		String actual3CharLeading = new String(Hex.decodeHex("4653450000000000000000000000000000000000")).trim();

		String actualHexStart = Strings.padEnd(new String(Hex.encodeHex("FSE".getBytes())), 40, '0');

		String actualHexLeading0 = codec.fromJson(DOUBLE_QUOTE + "FSE" + DOUBLE_QUOTE).toHex();

		Assertions.assertEquals("FSE", actual3CharMid);
		Assertions.assertEquals("FSE", actual3CharLeading);
		Assertions.assertEquals("4653450000000000000000000000000000000000", actualHexStart);
		Assertions.assertEquals("0000000000000000000000004653450000000000", actualHexLeading0);

	}

}
