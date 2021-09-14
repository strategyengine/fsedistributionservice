package com.strategyengine.xrpl.fsedistributionservice.service;

import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;

import lombok.NonNull;

public interface CurrencyHexService {

	boolean isAcceptedCurrency(FseTrustLine trustLine, String isoCurrency);

	boolean isAcceptedCurrency(@NonNull String currencyName);

}
