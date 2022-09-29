package com.strategyengine.xrpl.fsedistributionservice.entity.convert;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class KeyConverterTest {

	private KeyConverter sut = new KeyConverter();
	
	@BeforeEach
	public void setup() {
		sut.s = "blah";
	}
	@Test
	public void testConvert() {
		String key = "ED8B1116948F6231FEE76D7EB20721562544BE59D24BA999ECA4D27A489DB0TEST";
		String actual = sut.convertToDatabaseColumn(key);
		
		Assertions.assertEquals(key, sut.convertToEntityAttribute(actual));
		
	}
	
	

	@Test
	public void testConvert2() {
		String key = "AAAwrt#$%GfTEST";
		String actual = sut.convertToDatabaseColumn(key);

		System.out.println(actual);
		Assertions.assertEquals(key, sut.convertToEntityAttribute(actual));
		
	}

	@Test
	public void decTest() {

		String enc = "TESTQnk1X9St8za/9wwH5EFyGsX25MZdCfhCAA9viCgq3HIUkfh0XRUr0OvS2ftZf5huiNkHcWKxN2xO0pxyY0z5tPW32XZ2MgqkYCTEST=";
		String dec = sut.convertToEntityAttribute(enc);
		System.out.println(dec);
	}
}
