package com.strategyengine.xrpl.fsedistributionservice.model;

import java.util.List;

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
@ApiModel(value="POST parameters to send tokens to one or more recipients")
public class FsePaymentRequest {

	@ApiModelProperty(value="XRP address from wallet that is sending the token.  Example rnL2P..", required=true )
	@NonNull
	private String fromClassicAddress;

	@ApiModelProperty(value="XRP public signing address from wallet that is sending the token.  Example ED92AA5B7BBCE...", required=true)
	@NonNull	
	private String fromSigningPublicKey;

	@ApiModelProperty(value="XRP private key from wallet that is sending the token.  Example ED419C91A68F5...", required=true)	
	@NonNull
	private String fromPrivateKey;

	@ApiModelProperty(value="List of public XRP addresses that will each receive the tokens.  Example rnL2T..", required=true)
	@NonNull
	private List<String> toClassicAddresses;

	@ApiModelProperty(value="Amount of tokens that will be distributed to each recipient address.  Example .589", required=true)
	@NonNull
	private String amount;

	@ApiModelProperty(value="XRP destination tag if required.  NULLABLE Do not add attribute if null.  Example 12345")
	//nullable
	private String destinationTag;

	@ApiModelProperty(value="XRP address of the issuer of this currency.  Example rn2J..", required=true)
	@NonNull
	private String trustlineIssuerClassicAddress;

	@ApiModelProperty(value="Currency that is being distributed.  Example FSE", required=true)
	@NonNull
	private String currencyName;

}
