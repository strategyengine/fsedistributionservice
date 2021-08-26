package com.strategyengine.xrpl.fsedistributionservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Builder
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class FsePaymentTrustlinesRequest {

	@NonNull
	private String fromClassicAddress;
	@NonNull
	private String fromSigningPublicKey;
	@NonNull
	private String fromPrivateKey;
	
	//XRP address of token issuer
	@NonNull
	private String trustlineIssuerClassicAddress;
	
	//FSE
	@NonNull
	private String currencyName;
	@NonNull
	private String amount;
	
	//default false
	private boolean zeroBalanceOnly;
}
