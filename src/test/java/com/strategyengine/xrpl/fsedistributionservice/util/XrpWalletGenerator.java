package com.strategyengine.xrpl.fsedistributionservice.util;

import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.crypto.PrivateKey;
import org.xrpl.xrpl4j.keypairs.DefaultKeyPairService;
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

	static boolean isTest = false;

	public static void main(String[] args) throws JsonRpcClientErrorException {

		System.out.println("IS PROD WALLET? " + !isTest);

		Wallet wallet = null;
		String seed = null;
		seed = DefaultKeyPairService.getInstance().generateSeed();
		wallet = new XrpWalletGenerator().generateWallet(seed);

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

	private Wallet generateWallet(String seed) {

		// Create a Wallet using a WalletFactory
		WalletFactory walletFactory = DefaultWalletFactory.getInstance();
		final Wallet wallet = walletFactory.fromSeed(seed, isTest);

		return wallet;

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
