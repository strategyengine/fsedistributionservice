package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropType;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
import com.strategyengine.xrpl.fsedistributionservice.service.ConfigService;

public class ValidationServiceImplTest {

	private ValidationServiceImpl sut;

	@Mock
	private ConfigService configService;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		sut = new ValidationServiceImpl();
		sut.configService = configService;

		Mockito.when(configService.getDouble(AirdropVerificationServiceImpl.DEFAULT_SERVICE_FEE_PER_INTERVAL_VERFIED))
				.thenReturn(.1);
		Mockito.when(configService.getDouble(AirdropVerificationServiceImpl.DEFAULT_SERVICE_FEE_PER_INTERVAL_UNVERFIED))
				.thenReturn(1.0);
		Mockito.when(configService.getDouble(AirdropVerificationServiceImpl.SERVICE_FEE_INTERVAL_VERIFIED))
				.thenReturn(5000.0);
		Mockito.when(configService.getDouble(AirdropVerificationServiceImpl.SERVICE_FEE_INTERVAL_UNVERIFIED))
				.thenReturn(25000.0);
	}

	@Test
	public void testValidateFseBalanceNotEnough() {

		BadRequestException thrown = Assertions.assertThrows(BadRequestException.class, () -> {
			sut.validateFseBalance(.2, 20, DropType.SPECIFICADDRESSES, false);
		}, "BadRequestException was expected");

	}

	@Test
	public void testValidateFseBalanceNotEnoughZero() {

		BadRequestException thrown = Assertions.assertThrows(BadRequestException.class, () -> {
			sut.validateFseBalance(0.0, 20, DropType.SPECIFICADDRESSES, false);
		}, "BadRequestException was expected");

	}

	@Test
	public void testValidateFseBalanceMoreThanEnough() {

		sut.validateFseBalance(100.0, 20, DropType.SPECIFICADDRESSES, false);

	}

	@Test
	public void testValidateFseBalanceJustEnough() {

		sut.validateFseBalance(1.0, 20, DropType.SPECIFICADDRESSES, false);

	}

	@Test
	public void testValidateDistBalanceBalanceJustEnough() {

		sut.validateDistributingTokenBalance(Optional.of(FseTrustLine.builder().balance("800").build()), ".2", 4000, null, null);

	}

	@Test
	public void testValidateDistBalBalanceNotEnough() {

		BadRequestException thrown = Assertions.assertThrows(BadRequestException.class, () -> {
			sut.validateDistributingTokenBalance(Optional.of(FseTrustLine.builder().balance("800").build()), ".3",
					4000, "fromAddress", "currencyName");
		}, "BadRequestException was expected");

	}

	@Test
	public void testValidateDistBalBalanceNotEnoughZero() {

		BadRequestException thrown = Assertions.assertThrows(BadRequestException.class, () -> {
			sut.validateDistributingTokenBalance(Optional.of(FseTrustLine.builder().balance("0").build()), ".3", 4000, "fromAddress", "currencyName");
		}, "BadRequestException was expected");

	}

	@Test
	public void testMatch() {

		Assertions.assertTrue(sut.isValidClassicAddress("rfvg8YLnMZh1ezqquipiGvCQfu73FYRy5X"));
		Assertions.assertFalse(sut.isValidClassicAddress("rfvg8YLnMZ&h1ezqquipiGvCQfu73FYRy5X"));
		Assertions.assertFalse(sut.isValidClassicAddress("rfvg8YLnMZh*1ezqquipiGvCQfu73FYRy5X"));
		Assertions.assertFalse(sut.isValidClassicAddress("rfvg8YLnMZh1**ezqquipiGvCQfu73FYRy5X"));
		Assertions.assertFalse(sut.isValidClassicAddress("rfvg8YLnMZh1ez(qquipiGvCQfu73FYRy5X"));
		Assertions.assertFalse(sut.isValidClassicAddress("rfvg8YLnMZh1ezq@quipiGvCQfu73FYRy5X"));
		Assertions.assertFalse(sut.isValidClassicAddress("rfvg8YLnMZh1ezqq!uipiGvCQfu73FYRy5X"));
		Assertions.assertFalse(sut.isValidClassicAddress("rfvg8YLnMZh1ezqqu#ipiGvCQfu73FYRy5X"));
		Assertions.assertFalse(sut.isValidClassicAddress("rfvg8YLnMZh1ezqqpiGvCQfu73FYRy5X "));
	}

}
