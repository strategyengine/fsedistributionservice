package com.strategyengine.xrpl.fsedistributionservice.model;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class ScammerAddress implements Serializable {


	private static final long serialVersionUID = -285253566842082007L;

	private Long id;

	private String account;
	
	private Date createDate;
	
	private Date updateDate;
	
	private String type;
	
}
