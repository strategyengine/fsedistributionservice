package com.strategyengine.xrpl.fsedistributionservice.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "XRPL_TRANSACTION")
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class TransactionEnt {

	@Id
	@Column(name = "ID")
//	@GeneratedValue(strategy = GenerationType.AUTO)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_generator")
	@SequenceGenerator(name="transaction_generator", sequenceName = "transaction_id_seq", allocationSize=1)
	private Long id;

	@Column(name = "TX_HASH")
	private String hash;
	
	@Column(name = "CREATE_DATE")
	private Date createDate;
	
	@Column(name = "FAIL_REASON")
	private String failReason;

	@Column(name = "RESPONSE_CODE")
	private String code;
	

	@Column(name = "DROP_RECIPIENT_ID")
	private Long dropRecipientId;
	
	@Column(name = "DROP_REQUEST_ID")
	private Long dropRequestId;
	
	
}
