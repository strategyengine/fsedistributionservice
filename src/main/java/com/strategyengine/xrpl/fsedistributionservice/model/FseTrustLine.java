package com.strategyengine.xrpl.fsedistributionservice.model;

import java.io.Serializable;

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
public class FseTrustLine implements Serializable {


	private static final long serialVersionUID = 6932189011042862195L;

	private String classicAddress;
	
	private String currency;
	
	private String balance;
	
	private String limit;
	
}
