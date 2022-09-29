package com.strategyengine.xrpl.fsedistributionservice.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRecipientStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "DROP_RECIPIENT")
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class DropRecipientEnt {

	@Id
	@Column(name = "ID")
//	@GeneratedValue(strategy = GenerationType.AUTO)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "droprecip_generator")
	@SequenceGenerator(name="droprecip_generator", sequenceName = "drop_recipient_id_seq", allocationSize=1)
	private Long id;

	@Column(name = "ADDRESS")
	private String address;
	
	@Column(name = "CREATE_DATE")
	private Date createDate;
	
	@Column(name = "UPDATE_DATE")
	private Date updateDate;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "STATUS")
	private DropRecipientStatus status;
	
	@Column(name = "FAIL_REASON")
	private String failReason;

	@Column(name = "RESPONSE_CODE")
	private String code;
	
	@Column(name = "RETRY_ATTEMPT")
	private Integer retryAttempt;
	

	@Column(name = "DROP_REQUEST_ID")
	private Long dropRequestId;

	@Column(name = "SNAPSHOT_BALANCE")
	private String snapshotBalance;
	
	@Column(name = "PAY_AMOUNT")
	private String payAmount;
	
	
	
}
