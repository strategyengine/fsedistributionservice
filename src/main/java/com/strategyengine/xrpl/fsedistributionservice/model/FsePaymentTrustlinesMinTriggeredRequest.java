package com.strategyengine.xrpl.fsedistributionservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(value="POST parameters to schedule tokens dropped to all trustlines after a minimum number of trustlines is reached")
public class FsePaymentTrustlinesMinTriggeredRequest {

	@ApiModelProperty(value="Trustline payment request details", required=true)
	@NonNull
	private FsePaymentTrustlinesRequest trustlinePaymentRequest;
	
	@ApiModelProperty(value="Number of trustlines to wait for until the tokens will be distributed", required=true)
	private int minTrustLinesTriggerValue;
	
}
