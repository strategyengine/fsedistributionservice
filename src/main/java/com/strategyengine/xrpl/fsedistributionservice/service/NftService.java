package com.strategyengine.xrpl.fsedistributionservice.service;

import java.util.List;

import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.XrplDataNftDto;

public interface NftService {

	List<XrplDataNftDto> getNftOwners(FsePaymentRequest paymentRequest);

}
