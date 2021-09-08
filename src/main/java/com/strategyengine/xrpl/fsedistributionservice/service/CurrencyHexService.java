package com.strategyengine.xrpl.fsedistributionservice.service;

import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;

public interface CurrencyHexService {

	boolean isAcceptedCurrency(FseTrustLine trustLine, String isoCurrency);

}
