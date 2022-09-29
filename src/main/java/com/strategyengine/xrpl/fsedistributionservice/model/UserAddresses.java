package com.strategyengine.xrpl.fsedistributionservice.model;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder(toBuilder=true)
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class UserAddresses implements Serializable {

	private static final long serialVersionUID = -5852546486839305704L;
	private String uniqueid;
	private List<String> addresses;
}
