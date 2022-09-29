package com.strategyengine.xrpl.fsedistributionservice.client.xrp;

import com.strategyengine.xrpl.fsedistributionservice.model.XrpScanAccountResponse;

public interface XrpScanClient {

	XrpScanAccountResponse getAccount(String address);

	boolean isKycForAddress(String address);

}
