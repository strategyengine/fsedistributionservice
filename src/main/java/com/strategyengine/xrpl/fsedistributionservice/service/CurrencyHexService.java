package com.strategyengine.xrpl.fsedistributionservice.service;

import lombok.NonNull;

public interface CurrencyHexService {

	boolean isAcceptedCurrency(@NonNull String currencyName);

	String fixCurrencyCode(String isoCurrency);

	String currencyString(@NonNull String currencyName);

}
