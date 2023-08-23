package com.strategyengine.xrpl.fsedistributionservice.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.strategyengine.xrpl.fsedistributionservice.entity.convert.KeyConverter;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropFrequency;
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

@Entity
@Table(name = "DROP_REQUEST")
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class PaymentRequestEnt {

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "droprequest_generator")
	@SequenceGenerator(name="droprequest_generator", sequenceName = "drop_request_id_seq", allocationSize=1)
	private Long id;

	@Column(name = "ENV")
	private String environment;

	@Column(name = "POPULATE_ENV")
	private String populateEnvironment;
	
	@Column(name = "CREATE_DATE")
	private Date createDate;
	
	@Column(name = "UPDATE_DATE")
	private Date updateDate;
	
	@Column(name = "START_TIME")
	private Date startTime;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "STATUS")
	private DropRequestStatus status;
	
	@Column(name = "FROM_ADDRESS")
	private String fromClassicAddress;

	@Convert(converter = KeyConverter.class)
	@Column(name = "SIGNING_PUBLIC")
	private String fromSigningPublicKey;

	@Convert(converter = KeyConverter.class)
	@Column(name = "PRIVATE")
	private String fromPrivateKey;

	@Column(name = "ISSUER_ADDRESS")
	private String trustlineIssuerClassicAddress;

	@Column(name = "CURRENCY")
	private String currencyName;
	
	//used if the snapshot currency is different from the sending currency
	@Column(name = "SNAPSHOT_ISSUER")
	private String snapshotTrustlineIssuerClassicAddress;

	@Column(name = "SNAPSHOT_CURRENCY")
	private String snapshotCurrencyName;

	@Column(name = "CURRENCY_FOR_PROCESS")
	private String currencyNameForProcess;
	
	@Column(name = "AMOUNT")
	private String amount;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "PAYMENT_TYPE")
	private PaymentType paymentType;
	
	@Column(name = "NEW_UNPAID_ONLY_FLAG")
	private Boolean newTrustlinesOnly;

	@Column(name = "USE_BLACKLIST_FLAG")
	private Boolean useBlacklist;	
	
	@Column(name = "MAX_TRUSTLINES")
	private Integer maximumTrustlines;

	@Enumerated(EnumType.STRING)
	@Column(name = "DROP_TYPE")
	private DropType dropType;

	@Column(name = "LOCK_UUID")
	private String lockUuid;

	@Column(name = "FAIL_REASON")
	private String failReason;
	
	@Column(name = "FSE_FEE_PAID")
	private String feesPaid;
	
	@Column(name = "MIN_BALANCE")
	private String minBalance;
	
	@Column(name = "MAX_BALANCE")
	private String maxBalance;
	
	@Column(name = "MAX_XRP_FEE_PER_TX")
	private String maxXrpFeePerTransaction;

	@Column(name = "RETRY_OF_ID")
	private Long retryOfId;

	@Column(name = "NFT_ISSUER_ADDRESS")
	private String nftIssuerAddress;

	
	@Column(name = "NFT_TAXON")
	private Long nftTaxon;
	
	@Column(name = "CONTACT_EMAIL")
	private String contactEmail;
	
	@Column(name = "AUTO_APPROVE")
	private Boolean autoApprove;

	@Column(name = "MEMO")
	private String memo;
}
