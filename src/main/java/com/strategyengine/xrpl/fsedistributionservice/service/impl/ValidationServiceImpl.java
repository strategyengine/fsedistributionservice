package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.service.CurrencyHexService;
import com.strategyengine.xrpl.fsedistributionservice.service.ValidationService;

import lombok.NonNull;

@Service
public class ValidationServiceImpl implements ValidationService {
	
	@Autowired
	protected CurrencyHexService currencyHexService;

	@Override
	public void validateClassicAddress(String classicAddress) {

		if (classicAddress != null && classicAddress.startsWith("r")) {
			return;
		}

		throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"Expeted classic address but received " + classicAddress);

	}

	@Override
	public void validate(FsePaymentRequest payment) {

		if (payment.getDestinationTag() != null) {
			try {
				Integer.parseInt(payment.getDestinationTag());
			} catch (Exception e) {
				throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
						"Destination Tag is invalid.  Needs to be removed or set to a number");
			}
		}

		validateClassicAddress(payment.getFromClassicAddress());
		validateClassicAddress(payment.getTrustlineIssuerClassicAddress());
		payment.getToClassicAddresses().stream().forEach(a -> validateClassicAddress(a));
		;

		validateXAddress(payment.getFromPrivateKey());
		validateXAddress(payment.getFromSigningPublicKey());

		validateAmount(payment.getAmount());
		validateCurrency(payment.getCurrencyName());

	}

	private void validateCurrency(@NonNull String currencyName) {
		if(currencyName != null && currencyHexService.isAcceptedCurrency(currencyName)) {
			return;
		}
		throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid currency received " + currencyName);			
	}

	private void validateAmount(@NonNull String amount) {
		
		try {
			Double.parseDouble(amount);
		}catch(Exception e) {
			throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid amount received " + amount);			
		}
		
	}

	private void validateXAddress(@NonNull String key) {

		if (key != null && key.startsWith("E") && key.length() == 66) {
			return;
		}

		throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Expeted 66 character key but received " + key);

	}

	@Override
	public void validate(FsePaymentTrustlinesRequest payment) {

		validateClassicAddress(payment.getFromClassicAddress());
		validateClassicAddress(payment.getTrustlineIssuerClassicAddress());

		validateXAddress(payment.getFromPrivateKey());
		validateXAddress(payment.getFromSigningPublicKey());
		
		validateAmount(payment.getAmount());
		validateCurrency(payment.getCurrencyName());

	}

}
