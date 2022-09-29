package com.strategyengine.xrpl.fsedistributionservice.client.xrp;

import java.util.List;

import com.strategyengine.xrpl.fsedistributionservice.model.UserAddresses;

public interface GlobalIdClient {

	List<UserAddresses> getGlobalIdXrpAddresses();

}
