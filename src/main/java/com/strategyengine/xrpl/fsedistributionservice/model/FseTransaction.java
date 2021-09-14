package com.strategyengine.xrpl.fsedistributionservice.model;

import java.math.BigDecimal;
import java.util.Date;

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
public class FseTransaction {

	private Date transactionDate;
	private BigDecimal amount;
	private String toAddress;
	private String fromAddress;
	private String currency;
	private String issuerAddress;
	private String transactionType;
	private Long ledgerIndex;
}
