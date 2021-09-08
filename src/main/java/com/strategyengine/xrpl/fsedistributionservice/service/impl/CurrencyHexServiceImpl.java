package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.regex.Pattern;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.service.CurrencyHexService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class CurrencyHexServiceImpl implements CurrencyHexService {

	protected static final Pattern HEX_REGEX = Pattern.compile("^[A-Z0-9]{40}$");

	@Override
	public FseTrustLine convertCurrencyHexToCode(FseTrustLine trustLine) {

		try {
			if (isHex(trustLine.getCurrency())) {

				return FseTrustLine.builder().balance(trustLine.getBalance())
						.classicAddress(trustLine.getClassicAddress())
						.currency(convertHexToCurrencyCode(trustLine.getCurrency())).build();
			}
		} catch (Exception e) {
			log.error("Could not convert currency hex for trustline " + trustLine, e);
		}
		return trustLine;
	}

	private boolean isHex(String val) {

		return HEX_REGEX.matcher(val).matches();
	}

	private String convertHexToCurrencyCode(String hex) throws DecoderException {

		return new String(Hex.decodeHex(hex)).trim();
	}
}
