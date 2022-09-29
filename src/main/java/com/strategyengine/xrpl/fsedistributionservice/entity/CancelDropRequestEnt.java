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
@Table(name = "CANCEL_DROP_REQUEST")
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class CancelDropRequestEnt {

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "canceldroprequest_generator")
	@SequenceGenerator(name="canceldroprequest_generator", sequenceName = "canceldroprequest_id_seq", allocationSize=1)
	private Long id;

	
	@Column(name = "CREATE_DATE")
	private Date createDate;
	

	@Column(name = "DROP_REQUEST_ID")
	private Long dropRequestId;
	
	
}
