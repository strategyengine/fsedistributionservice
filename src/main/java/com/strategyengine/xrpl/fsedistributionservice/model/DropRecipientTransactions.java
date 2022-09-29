package com.strategyengine.xrpl.fsedistributionservice.model;

import java.io.Serializable;
import java.util.List;

import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;

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
public class DropRecipientTransactions implements Serializable {

	private static final long serialVersionUID = 5782904866498841291L;

	private DropRecipientEnt dropRecipient;
	
	private List<FseTransaction> transactions;
	
	private List<FseTransaction> transactionsFromMap;
	
	private boolean didReceive;
	
}
