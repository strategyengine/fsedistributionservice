package com.strategyengine.xrpl.fsedistributionservice.model;

import java.io.Serializable;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
public class PaymentsChange implements Serializable {

	private static final long serialVersionUID = -479433688201771621L;

	@ApiModelProperty(value="ID of the airdrop", required=true)
	private Long dropRequestId;
	
	@ApiModelProperty(value="Private key used when creating the drop", required=true)
	private String privateKey;

	@ApiModelProperty(value="XRP addresses that will each receive the tokens.  Example rnL2T..", required=true)
	private List<PaymentAmountChange> paymentAmountChanges;


}
