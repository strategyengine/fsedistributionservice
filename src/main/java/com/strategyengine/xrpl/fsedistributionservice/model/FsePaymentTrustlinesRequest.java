package com.strategyengine.xrpl.fsedistributionservice.model;

import com.strategyengine.xrpl.fsedistributionservice.service.impl.XrplServiceImpl;

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

@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
@ApiModel(value = "POST parameters to send tokens addresses with trustlines for the token")
public class FsePaymentTrustlinesRequest {

	@ApiModelProperty(value = "XRP address from wallet that is sending the token.  Example rnL2P..", required = true)
	@NonNull
	private String fromClassicAddress;

	@ApiModelProperty(value = "XRP public signing address from wallet that is sending the token.  Example ED92AA5B7BBCE...", required = true)
	@NonNull
	private String fromSigningPublicKey;

	@ApiModelProperty(value = "XRP private key from wallet that is sending the token.  Example ED419C91A68F5...", required = true)
	@NonNull
	private String fromPrivateKey;

	@ApiModelProperty(value = "XRP address of the issuer of this currency.  Example rn2J..", required = true)
	@NonNull
	private String trustlineIssuerClassicAddress;

	@ApiModelProperty(value = "Currency that is being distributed.  Example FSE", required = true)
	@NonNull
	private String currencyName;

	@ApiModelProperty(value = "Amount of tokens that will be distributed to each recipient address.  Example .589", required = true)
	@NonNull
	private String amount;

	@ApiModelProperty(value = "If true, then only recipients who have never recieved one of your airdrops will receive one.  DEFAULT VALUE: FALSE")
	private boolean newTrustlinesOnly;

	@ApiModelProperty(value = "I agree to the " + XrplServiceImpl.SERVICE_FEE
			+ " XRP fee to use this service", required = true)
	@NonNull
	private boolean agreeFee;

	@ApiModelProperty(value = "OPTIONAL:  The maximum number of trustlines to send to.   Sorted by oldest, so if set to 10 then only the oldest 10 trustlines will receive this drop.  If set to 50000 but there are only 10000 actual trustlines, then all 10000 will receive the drop.   You can leave the attribute out to send to everyone", required = false)
	private Integer maximumTrustlines;
	
	@ApiModelProperty(value="If true, then this payment will also go to blacklisted addresses", required=true)
	private boolean payBlacklistedAddresses;

}
