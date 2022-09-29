package com.strategyengine.xrpl.fsedistributionservice.entity.convert;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.strategyengine.xrpl.fsedistributionservice.service.ConfigService;

@Service
@Converter
public class KeyConverter implements AttributeConverter<String, String> {

	private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
	
	
	@Autowired
	private ConfigService configService;
	
	protected static String s;
	
	@PostConstruct
	public void init() {
		s = configService.getSomeSecSaltySauce();
	}

	@Override
	public String convertToDatabaseColumn(String v) {

		try {
			return encAdd(v);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String convertToEntityAttribute(String db) {
		try {
			return decAdd(db);
		} catch (Exception e) {
			return null;
		}
	}

	public static SecretKey sec() throws NoSuchAlgorithmException, InvalidKeySpecException {

		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		KeySpec spec = new PBEKeySpec(s.toCharArray(), s.getBytes(), 65536, 256);
		SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
		return secret;
	}

	public static IvParameterSpec iv() {
		byte[] iv = new byte[16];
		byte[] v = "a1".getBytes();
		for (int i = 0; i < v.length; i++) {
			iv[i] = v[i];
		}
		return new IvParameterSpec(iv);
	}

	public static String encAdd(String plainText) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, sec(), iv());
		return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes()));
	}

	public static String decAdd(String cipherText) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		cipher.init(Cipher.DECRYPT_MODE, sec(), iv());
		return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)));
	}

}