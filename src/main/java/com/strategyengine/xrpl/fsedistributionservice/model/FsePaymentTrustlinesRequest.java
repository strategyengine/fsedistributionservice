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
@ApiModel(value="POST parameters to send tokens addresses with trustlines for the token")
public class FsePaymentTrustlinesRequest {

	@ApiModelProperty(value="XRP address from wallet that is sending the token.  Example rnL2P..", required=true )
	@NonNull
	private String fromClassicAddress;
	
	@ApiModelProperty(value="XRP public signing address from wallet that is sending the token.  Example ED92AA5B7BBCE...", required=true)
	@NonNull
	private String fromSigningPublicKey;
	
	@ApiModelProperty(value="XRP private key from wallet that is sending the token.  Example ED419C91A68F5...", required=true)	
	@NonNull
	private String fromPrivateKey;
	
	@ApiModelProperty(value="XRP address of the issuer of this currency.  Example rn2J..", required=true)
	@NonNull
	private String trustlineIssuerClassicAddress;
	
	@ApiModelProperty(value="Currency that is being distributed.  Example FSE", required=true)
	@NonNull
	private String currencyName;
	
	@ApiModelProperty(value="Amount of tokens that will be distributed to each recipient address.  Example .589", required=true)
	@NonNull
	private String amount;
	
	@ApiModelProperty(value="If true, then only recipients with zero balances of the token will receive tokens.  DEFAULT VALUE: FALSE")
	private boolean zeroBalanceOnly;
}
