package com.strategyengine.xrpl.fsedistributionservice.model;

import java.math.BigDecimal;
import java.util.List;

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
public class FseAccount {

	private String classicAddress;
	
	private BigDecimal balance;
	
	private List<FseTrustLine> trustLines;
}
