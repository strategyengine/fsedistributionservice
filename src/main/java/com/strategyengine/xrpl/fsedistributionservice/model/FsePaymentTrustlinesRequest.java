package com.strategyengine.xrpl.fsedistributionservice.model;

import java.io.Serializable;
import java.util.Date;

import com.strategyengine.xrpl.fsedistributionservice.entity.types.PaymentType;

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
public class FsePaymentTrustlinesRequest implements Serializable {

	private static final long serialVersionUID = 8073142123190892467L;

	@ApiModelProperty(value = "XRP address from wallet that is sending the token.  Example rnL2P..", required = true)
	@NonNull
	private String fromClassicAddress;

	@ApiModelProperty(value = "XRP public signing address from wallet that is sending the token.  Example ED92AA5B7BBCE...", required = true)
	@NonNull
	private String fromSigningPublicKey;

	@ApiModelProperty(value = "XRP private key from wallet that is sending the token.  Example ED419C91A68F5...", required = true)
	@NonNull
	@ToString.Exclude
	private String fromPrivateKey;

	@ApiModelProperty(value = "XRP address of the issuer of this currency.  Example rn2J..", required = true)
	@NonNull
	private String trustlineIssuerClassicAddress;

	@ApiModelProperty(value = "Currency that is being distributed.  Example FSE", required = true)
	@NonNull
	private String currencyName;

	@ApiModelProperty(value = "Each trustline will receive this amount.  Example .589", required = true)
	@NonNull
	private String amount;

	@ApiModelProperty(value = "If true, then only recipients who have never recieved one of your airdrops will receive one.  DEFAULT VALUE: FALSE")
	private boolean newTrustlinesOnly;

	@ApiModelProperty(value = "If true, then only recipients who have been verified with Global.ID will receive the airdrop")
	private boolean globalIdVerified;
	
	@ApiModelProperty(value = "OPTIONAL: If true, then the blacklist will be used to filter scam addresses")
	private boolean useBlacklist;

	@ApiModelProperty(value="I agree to the service fee per recipients to use this service", required=true)
	@NonNull
	private boolean agreeFee;

	@ApiModelProperty(value = "OPTIONAL:  The maximum number of trustlines to send to.   Sorted by oldest, so if set to 10 then only the oldest 10 trustlines will receive this drop.  If set to 50000 but there are only 10000 actual trustlines, then all 10000 will receive the drop.   You can leave the attribute out to send to everyone", required = false)
	private Integer maximumTrustlines;

	@ApiModelProperty(value = "OPTIONAL:  Only pay trustlines having a token balance of the value or more.", required = false)
	private Double minBalance;

	@ApiModelProperty(value = "OPTIONAL:  Only pay trustlines having a token balance of the value or less.", required = false)	
	private Double maxBalance;
	
	@ApiModelProperty(value="OPTIONAL: The max XRP fee you are willing to pay per transaction", required=false)
	private String maxXrpFeePerTransaction;
	
	@ApiModelProperty(value = "OPTIONAL: Retry of other airdrop id")
	private Long retryOfId;
	
	@ApiModelProperty(value = "FLAT or PROPORTIONAL.  Flat payments pay the exact amount to each recipient.  Proportional pays the amount * the recipients balance")
	private PaymentType paymentType;

	@ApiModelProperty(value = "XRP address of the issuer of this currency if different from the sending currency.  Example rn2J..", required = true)
	private String snapshotTrustlineIssuerClassicAddress;

	@ApiModelProperty(value = "Currency used for the snapshot if different from the sending currency.  Example FSE", required = true)
	private String snapshotCurrencyName;

	@ApiModelProperty(value = "The point after which this airdrop can begin", required = false)
	private Date startTime;

}
