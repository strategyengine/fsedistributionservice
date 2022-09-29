package com.strategyengine.xrpl.fsedistributionservice.model;

import java.io.Serializable;
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
public class FseTransaction implements Serializable {


	private static final long serialVersionUID = -7014861027169979326L;
	private Date transactionDate;
	private BigDecimal amount;
	private String toAddress;
	private String fromAddress;
	private String currency;
	private String issuerAddress;
	private String transactionType;
	private Long ledgerIndex;
	private String transactionHash;
	private String resultCode;
	private String reason;
}
