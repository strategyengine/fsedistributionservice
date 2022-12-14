package com.strategyengine.xrpl.fsedistributionservice.entity.types;

/**
 * FLAT payment will pay the recipient the amount exactly.  Each airdrop recipient will receive the amount for the drop
 * PROPORTIONAL payment will pay the recipient a proportion of their balance.  The amount * their balance.    So an amount of .1 * a balance of 5000 will receive 500
 * @author barry
 *
 */
public enum DropFrequency {

	DAILY, WEEKLY, MONTHLY, ANNUALLY
}
