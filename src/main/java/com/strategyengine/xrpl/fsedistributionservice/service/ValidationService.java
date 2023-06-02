package com.strategyengine.xrpl.fsedistributionservice.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropType;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;

import lombok.NonNull;

public interface ValidationService {

	void validateClassicAddress(String classicAddress);

	void validateClassicAddressAccountLookup(String classicAddress, String field);

	void validate(FsePaymentRequest payment);

	void validate(FsePaymentTrustlinesRequest p);

	void validateXrpBalance(BigDecimal balance, int size);

	boolean isValidClassicAddress(String classicAddress);

	void validateAirdropNotAlreadyQueuedForIssuer(String issuingAddress);

	void validateAirdropNotAlreadyQueuedForFromAddress(String fromAddress);

	void validateFseBalance(Double fseBlance, int size, DropType dropType, boolean crossCurrencyDrop);

	void validateDistributingTokenBalance(Optional<FseTrustLine> fromAddressTrustLine, @NonNull String amount, int size,
			String fromAddress, String currencyName);

	
}
