package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableList;
import com.strategyengine.xrpl.fsedistributionservice.model.FseAccount;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

@RestController
public class AnalysisControllerTest {

	@Mock
	private XrplService xrplService;

	private AnalysisController sut;

	private String classicAddress = "vincent";

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		sut = new AnalysisController();
		sut.xrplService = xrplService;
	}

	@Test
	public void testTrustLines() {
		List<FseTrustLine> expected = trustLines();
		
		Mockito.when(xrplService.getTrustLines(classicAddress, Optional.empty(), true, true)).thenReturn(expected);

		List<FseTrustLine> actual = sut.trustLines(classicAddress, null, true);

		Assertions.assertEquals(expected, actual);
	}

	private List<FseTrustLine> trustLines() {
		return ImmutableList.of(Mockito.mock(FseTrustLine.class));
	}

	@Test
	public void testAccountInfo() {
		List<FseAccount> expected = ImmutableList.of( accountInfo());
		
		Mockito.when(xrplService.getAccountInfo(ImmutableList.of(classicAddress))).thenReturn(expected);

		List<FseAccount> actual = sut.accountInfo(ImmutableList.of(classicAddress));

		Assertions.assertEquals(expected, actual);
	}

	private FseAccount accountInfo() {
		return Mockito.mock(FseAccount.class);
	}

}