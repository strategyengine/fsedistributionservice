package com.strategyengine.xrpl.fsedistributionservice.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

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
public class XrpScanAccountResponse implements Serializable {

	
	//{"sequence":65753632,"xrpBalance":"14107.262631","ownerCount":5,"previousAffectingTransactionID":"B4C70D9535BBE3043A6A241C17B92AD20418990DA07853C2024B4DDBC4ADD4A2",
	//	"previousAffectingTransactionLedgerVersion":66986948,"settings":{},"account":"rsmbyyYLwtRBKCdwLmbwpemuvmU9wVNhpY","parent":"rp7TSXHQ3XrZQYaCVZdeeaJLeAPHYpb1Ju","initial_balance":8386.191996,"inception":"2021-08-18T23:41:50.000Z","ledger_index":65753578,"tx_hash":"068A97E14DB8FAD0D4D5F1722B9E2D1276D0961EF6D1D414265928230F572FD9","accountName":null,"parentName":null}
	
	private static final long serialVersionUID = -4488461848776818299L;
	private Long sequence;
	private String xrpBalance;
	private Integer ownerCount;
	private String previousAffectingTransactionID;
	private String previousAffectingTransactionLedgerVersion;
	private String account;
	private String parent;
	private BigDecimal initial_balance;
	private Date inception;
	private Long ledger_index;
	private XrpScanAccountName accountName;
	private XrpScanAccountName parentName;
	private Boolean kycApproved;
	
	
}

