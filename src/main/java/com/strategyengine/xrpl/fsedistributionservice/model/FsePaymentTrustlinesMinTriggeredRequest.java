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
public class FsePaymentTrustlinesMinTriggeredRequest {

	@NonNull
	private FsePaymentTrustlinesRequest trustlinePaymentRequest;
	
	private int minTrustLinesTriggerValue;
	
}
