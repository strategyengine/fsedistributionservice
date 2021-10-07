package com.strategyengine.xrpl.fsedistributionservice.model;

import java.util.Date;
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
public class FsePaymentResults {

	private List<FsePaymentResult> results;
	
	private Date start;
	private Date end;
	private int transactionCount;
}
