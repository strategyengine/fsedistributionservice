package com.strategyengine.xrpl.fsedistributionservice.entity.convert;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xrpl.xrpl4j.crypto.PrivateKey;
import org.xrpl.xrpl4j.keypairs.KeyPair;
import org.xrpl.xrpl4j.wallet.DefaultWalletFactory;
import org.xrpl.xrpl4j.wallet.Wallet;
import org.xrpl.xrpl4j.wallet.WalletFactory;

public class BaseEncodingPrivateKeyTest {
	
	String publickey = "";
	  String key = "";
	@Test
	public void testEncoding() {

		WalletFactory walletFactory = DefaultWalletFactory.getInstance();
		Wallet wallet = walletFactory.fromKeyPair(
				KeyPair.builder().privateKey(key)
						.publicKey(publickey).build(),
				false);
		
		System.out.print(wallet);


		PrivateKey privateKey = PrivateKey.fromBase16EncodedPrivateKey(key);
		
		
		System.out.println(privateKey);
		
		
		
	}

	
}
