package com.strategyengine.xrpl.fsedistributionservice.model;

import java.util.List;

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
public class FsePaymentRequest {

	@NonNull
	private String fromClassicAddress;
	@NonNull	
	private String fromSigningPublicKey;
	@NonNull
	private String fromPrivateKey;
	@NonNull
	private List<String> toClassicAddresses;
	@NonNull
	private String amount;
	//nullable
	private String destinationTag;
	
	//XRP address of token issuer
	@NonNull
	private String trustlineIssuerClassicAddress;
	
	//FSE
	@NonNull
	private String currencyName;

}
