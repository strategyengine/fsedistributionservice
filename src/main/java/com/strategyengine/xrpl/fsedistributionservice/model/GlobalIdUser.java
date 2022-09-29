package com.strategyengine.xrpl.fsedistributionservice.model;

import java.io.Serializable;

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
public class GlobalIdUser implements Serializable {

	private static final long serialVersionUID = 1988855796169913379L;
	private XrpTokenAddress xrpTokenAddress;
	private Integer totalVerifications;

}
