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
public class XrpScanAccountName implements Serializable {

	//{"name":"CoinSpot","account":"rBgnUKAEiFhCRLPoYNPPe3JUWayRjP6Ayg","domain":"coinspot.com.au","twitter":"coinspotau"}
	
	private static final long serialVersionUID = 5428674469862944428L;
	private String issuer;
	private Boolean kyc;
}
