package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.regex.Pattern;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.strategyengine.xrpl.fsedistributionservice.service.CurrencyHexService;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class CurrencyHexServiceImpl implements CurrencyHexService {

	protected static final Pattern HEX_REGEX = Pattern.compile("^[A-Z0-9]{40}$");
	protected static final Pattern ISO_REGEX = Pattern.compile("^[a-zA-Z0-9]{3}$");
	
	private boolean isHex(String val) {

		return HEX_REGEX.matcher(val).matches();
	}

	@Override
	public boolean isAcceptedCurrency(@NonNull String currencyName) {
		if (currencyName != null && currencyName.length() > 1) {
			return true;
		}

		return false;
	}

	@Override
	public String fixCurrencyCode(String val) {
		if (isHex(val) || isIsoCode(val)) {

			return val;
		}

		return Strings.padEnd(new String(Hex.encodeHex(val.getBytes())), 40, '0').toUpperCase();
	}


	private boolean isIsoCode(String iso) {
		return ISO_REGEX.matcher(iso).matches();
	}

	@Override
	public String currencyString(String val) {
		if(val==null) {
			return null;
		}
		if (isHex(val)) {
			try {
				return new String(Hex.decodeHex(val.toCharArray()));
			} catch (DecoderException e) {
				log.error("Failed to decode currency hex for " + val, e);
			}
		}
		return val;
	}
}
