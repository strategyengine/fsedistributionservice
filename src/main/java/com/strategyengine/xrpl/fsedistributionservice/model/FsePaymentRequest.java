package com.strategyengine.xrpl.fsedistributionservice.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropFrequency;
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

@Builder(toBuilder=true)
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
@ApiModel(value="POST parameters to send tokens to one or more recipients")
public class FsePaymentRequest implements Serializable {

	private static final long serialVersionUID = -479433688201661621L;

	@ApiModelProperty(value="XRP address from wallet that is sending the token.  Example rnL2P..", required=true )
	@NonNull
	private String fromClassicAddress;

	@ApiModelProperty(value="XRP public signing address from wallet that is sending the token.  Example ED92AA5B7BBCE...", required=true)
	@NonNull	
	private String fromSigningPublicKey;

	@ApiModelProperty(value="XRP private key from wallet that is sending the token.  Example ED419C91A68F5...", required=true)	
	@NonNull
	@ToString.Exclude
	private String fromPrivateKey;

	@ApiModelProperty(value="List of public XRP addresses that will each receive the tokens.  Example rnL2T..", required=true)
	private List<String> toClassicAddresses;

	@ApiModelProperty(value="Amount of tokens that will be distributed to each recipient address.  Example .589", required=true)
	@NonNull
	private String amount;
	
	@ApiModelProperty(value = "The point after which this airdrop can begin", required = false)
	private Date startTime;

	@ApiModelProperty(value="XRP destination tag if required.  NULLABLE Do not add attribute if null.  Value must be a number.  Example 12345")
	//nullable
	private String destinationTag;

	@ApiModelProperty(value="XRP address of the issuer of this currency.  Example rn2J..", required=true)
	@NonNull
	private String trustlineIssuerClassicAddress;

	@ApiModelProperty(value="Currency that is being distributed.  Example FSE", required=true)
	@NonNull
	private String currencyName;

	@ApiModelProperty(value="I agree to the service fee per recipients fee to use this service", required=true)
	private boolean agreeFee;

	@ApiModelProperty(value="OPTIONAL: The max XRP fee you are willing to pay per transaction", required=false)
	private String maxXrpFeePerTransaction;
	
	@ApiModelProperty(value = "OPTIONAL: If true, then only recipients who have been verified with Global.ID will receive the airdrop")
	private boolean globalIdVerified;

	@ApiModelProperty(value = "OPTIONAL: If true, then the blacklist will be used to filter scam addresses")
	private boolean useBlacklist;
	
	@ApiModelProperty(value = "OPTIONAL: Retry of other airdrop id")
	private Long retryOfId;
	
	@ApiModelProperty(value = "FLAT or PROPORTIONAL.  Flat payments pay the exact amount to each recipient.  Proportional pays the amount * the recipients balance")
	private PaymentType paymentType;
	
	@ApiModelProperty(value = "XRP address of the issuer of this currency if different from the sending currency.  Example rn2J..", required = true)
	private String snapshotTrustlineIssuerClassicAddress;

	@ApiModelProperty(value = "Currency used for the snapshot if different from the sending currency.  Example FSE", required = true)
	private String snapshotCurrencyName;

	@ApiModelProperty(value = "NFT issuing address used to fetch NFT owners", required = false)
	private String nftIssuingAddress;
	
	@ApiModelProperty(value = "NFT taxon used to filter NFT owners by the taxon", required = false)
	private Long nftTaxon;

	@ApiModelProperty(value = "If drop is recurring, frequncy is DAILY, WEEKLY, MONTHLY, ANNUALLY", required = false)
	private DropFrequency frequency;

	@ApiModelProperty(value = "If there is a frequency, repeat until this date.  If any drop fails, you will need to recreate the job.  No other repetitions will occur", required = false)
	private Date repeatUntilDate;
	
	@ApiModelProperty(value = "Email contact", required = false)
	private String email;


}
