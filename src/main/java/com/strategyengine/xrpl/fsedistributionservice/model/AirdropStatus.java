package com.strategyengine.xrpl.fsedistributionservice.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRequestStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropType;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.PaymentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class AirdropStatus implements Serializable {

	private static final long serialVersionUID = -1499856688503491441L;

	private List<FsePaymentResult> results;
	
	private Long id;

	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private Date createDate;
	
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private Date updateDate;

	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private Date startTime;
	
	private DropRequestStatus status;
	
	private String fromClassicAddress;

	private String trustlineIssuerClassicAddress;

	private String currencyName;

	private String currencyNameForProcess;
	
	private String amount;

	private boolean newTrustlinesOnly;
	
	private boolean useBlacklist;

	private Integer maximumTrustlines;

	private DropType dropType;

	private String failReason;

	private String minBalance;

	private String maxBalance;
	
	private Long totalBlacklisted;
	
	private String maxXrpFeePerTransaction;
	
	private Long totalRecipients;
	
	private PaymentType paymentType;
	
	private String snapshotTrustlineIssuerClassicAddress;
	
	private String snapshotCurrencyName;
	
	private String nftIssuingAddress;
	
	private String nftTaxon;
	
}
