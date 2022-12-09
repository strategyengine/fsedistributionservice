package com.strategyengine.xrpl.fsedistributionservice.model;

import java.io.Serializable;

import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRecipientStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.PaymentType;

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
public class FsePaymentResult implements Serializable {


	private static final long serialVersionUID = 8565362551585054692L;
	
	private Long id;
	private String responseCode;
	private String reason;
	private String classicAddress;
	private DropRecipientStatus status;
	private String paymentAmount;
	private String snapshotBalance;
	private String nftOwned;
	
}
