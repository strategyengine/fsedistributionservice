package com.strategyengine.xrpl.fsedistributionservice.model;

import java.io.Serializable;

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

@Builder(toBuilder=true)
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
@ApiModel(value="Change the amount to pay an address")
public class PaymentAmountChange implements Serializable {

	private static final long serialVersionUID = -479433688201661631L;


	@ApiModelProperty(value="XRP addresses that will each receive the tokens.  Example rnL2T..", required=true)
	private String toClassicAddress;

	@ApiModelProperty(value="Amount of tokens that will be distributed to each recipient address.  Example .589", required=true)
	@NonNull
	private String amount;


}
