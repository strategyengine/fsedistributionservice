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
@Table(name = "DROP_SCHEDULE_RUN")
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class DropScheduleRunEnt {

	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "drop_schedule_run_generator")
	@SequenceGenerator(name="drop_schedule_run_generator", sequenceName = "drop_schedule_run_id_seq", allocationSize=1)
	private Long id;
	
	@Column(name = "DROP_SCHEDULE_ID")
	private Long dropScheduleId;

	@Column(name = "DROP_REQUEST_ID")
	private Long dropRequestId;
	
	@Column(name = "CREATE_DATE")
	private Date createDate;
		
}
