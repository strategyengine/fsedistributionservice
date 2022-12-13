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

import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropFrequency;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropScheduleStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "DROP_SCHEDULE")
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class DropScheduleEnt {

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "drop_schedule_generator")
	@SequenceGenerator(name="drop_schedule_generator", sequenceName = "drop_schedule_id_seq", allocationSize=1)
	private Long id;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "FREQUENCY")
	private DropFrequency frequency;
	
	@Column(name = "REPEAT_UNTIL_DATE")
	private Date repeatUntilDate;
	
	@Column(name = "CREATE_DATE")
	private Date createDate;

	@Column(name = "LOCK_UUID")
	private String lockUuid;

	//date that the final schedule ran
	@Column(name = "STATUS")
	@Enumerated(EnumType.STRING)
	private DropScheduleStatus dropScheduleStatus;
	
	//entry in drop_request table with a status of scheduled from which all scheduled runs are based
	@Column(name = "DROP_REQUEST_ID")
	private Long dropRequestId;
	
}
