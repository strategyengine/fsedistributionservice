package com.strategyengine.xrpl.fsedistributionservice.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "BURN_TRANSACTION")
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class BurnTransactionEnt {

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(name = "TX_HASH")
	private String transactionHash;

	@Column(name = "LEDGER_INDEX")
	private Long ledgerIndex;
	
	@Column(name = "CREATE_DATE")
	private Date createDate;

	@Column(name = "TX_DATE")
	private Date txDate;
	
	@Column(name = "TO_ADDRESS")
	private String toAddress;


	@Column(name = "FROM_ADDRESS")
	private String fromAddress;
	
	@Column(name = "AMOUNT")
	private String amount;


}
