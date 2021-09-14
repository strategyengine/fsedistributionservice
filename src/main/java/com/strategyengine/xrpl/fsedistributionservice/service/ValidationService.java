package com.strategyengine.xrpl.fsedistributionservice.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;

import lombok.NonNull;

public interface ValidationService {

	void validateClassicAddress(String classicAddress);

	void validate(FsePaymentRequest payment);

	void validate(FsePaymentTrustlinesRequest p);

	void validateXrpBalance(BigDecimal balance, int size);

	void validateDistributingTokenBalance(Optional<FseTrustLine> fromAddressTrustLine, @NonNull String amount,
			int size);

}
