package com.strategyengine.xrpl.fsedistributionservice.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xrpl.xrpl4j.crypto.PrivateKey;
import org.xrpl.xrpl4j.keypairs.DefaultKeyPairService;
import org.xrpl.xrpl4j.keypairs.KeyPair;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.XAddress;
import org.xrpl.xrpl4j.wallet.DefaultWalletFactory;
import org.xrpl.xrpl4j.wallet.Wallet;
import org.xrpl.xrpl4j.wallet.WalletFactory;

/**
 * Generates a new wallet and outputs the keys and seed. You'll need to know
 * these values of your wallet in order to use the API
 * 
 * @author barry
 *
 */
public class XrpWalletGenerator {

	@Test
	public void vanity() {

		Wallet wallet = null;
		String seed = null;

		for (int i = 0; i < 100000000; i++) {
			seed = DefaultKeyPairService.getInstance().generateSeed();
			wallet = new XrpWalletGenerator().generateWallet(seed);
			String add = wallet.classicAddress().value();
	
			if (add.toUpperCase().endsWith("Y")||add.toUpperCase().endsWith("TE5T")) {
				break;
			}
		}
		// Get the Classic and X-Addresses from testWallet
		final Address classicAddress = wallet.classicAddress();

		final XAddress xAddress = wallet.xAddress();
		final String privateKeyEncoded = wallet.privateKey().get();
		PrivateKey privateKeyFrom16Encoded = PrivateKey.fromBase16EncodedPrivateKey(wallet.privateKey().get());

		String pkeyFromUnsignedByteArray = unsignedByteArrayToString(privateKeyFrom16Encoded.value().toByteArray());

		System.out.print(wallet);
		System.out.println("privateKey Base58Encoded: " + privateKeyFrom16Encoded.base58Encoded());
		System.out.println("Seed to regenerate: " + seed);

	}

	private static boolean check(String add, String v) {

		if (add.endsWith(v)) {
			return true;
		}
		if (add.startsWith("r" + v)) {
			return true;
		}

		return false;
	}

	private Wallet generateWallet(String seed) {

		// Create a Wallet using a WalletFactory
		WalletFactory walletFactory = DefaultWalletFactory.getInstance();

		final Wallet wallet = walletFactory.fromSeed(seed, false);

		return wallet;

	}

	@Test
	public void testFromSeed() {

		// Create a Wallet using a WalletFactory
		WalletFactory walletFactory = DefaultWalletFactory.getInstance();

		final Wallet wallet = walletFactory.fromSeed("sEdTzQVpXCtbPHBkmaESJ9dzJmVhDRz", false);

		Assertions.assertEquals("rnWyK1JZ5N8Z1n111hZrzTkyt973b6z2A7", wallet.classicAddress().value());

		System.out.println(wallet);
		
	}		

	@Test
	public void walletfromkeypair() {

		WalletFactory walletFactory = DefaultWalletFactory.getInstance();
		Wallet wallet = walletFactory.fromKeyPair(
				KeyPair.builder().privateKey("EDAF5B9601CF2AC5F7B31531299E223B32784429C101C6F564760E05C590E90EF1")
						.publicKey("EDF35E7E53FDA4E1B2DB50D03CC85048DF59FA87584EAC559976490D6C37AE598B").build(),
				false);

		Assertions.assertEquals("rnWyK1JZ5N8Z1n111hZrzTkyt973b6z2A7", wallet.classicAddress().value());
		
		System.out.print(wallet);

	}

	private static String unsignedByteArrayToString(byte[] byteArray) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < byteArray.length; i++) {

			char character = (char) byteArray[i];
			sb.append(character);
		}

		return sb.toString();
	}

}
