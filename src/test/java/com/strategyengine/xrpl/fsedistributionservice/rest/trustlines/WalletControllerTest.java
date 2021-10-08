package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.web.bind.annotation.RestController;

import com.strategyengine.xrpl.fsedistributionservice.model.FseWallet;
import com.strategyengine.xrpl.fsedistributionservice.rest.trustlines.WalletController;

@RestController
public class WalletControllerTest {


	private WalletController sut;


	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		sut = new WalletController();
	}


	@Test
	public void testWalletGen() {


		FseWallet actual = sut.generateWallet();
		
		Assertions.assertNotNull(actual.getFromPrivateKey());
	}
}