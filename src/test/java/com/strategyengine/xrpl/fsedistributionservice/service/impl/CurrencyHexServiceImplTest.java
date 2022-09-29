package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.xrpl.xrpl4j.codec.binary.types.CurrencyType;

import com.google.common.base.Strings;

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
	public void testOne() {

		String actual = sut.fixCurrencyCode("KATANA");

		Assertions.assertEquals("4B4154414E410000000000000000000000000000", actual);



	}
	@Test
	public void fixThreeLetterCode() {

		String actual = sut.fixCurrencyCode("xSD");

		Assertions.assertEquals("xSD", actual);
	}	
	

	@Test
	public void fixCurrencyCode() {

		String actual = sut.fixCurrencyCode("FGARY");

		Assertions.assertEquals("4647415259000000000000000000000000000000", actual);

		actual = sut.fixCurrencyCode("Fractal");

		Assertions.assertEquals("4672616374616C00000000000000000000000000", actual);

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
