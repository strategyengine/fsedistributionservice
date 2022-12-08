package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplNftClient;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.XrplDataNftDto;
import com.strategyengine.xrpl.fsedistributionservice.service.NftService;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class NftServiceImpl implements NftService {

	@VisibleForTesting
	@Autowired
	protected XrplNftClient xrplNftClient;


	@Transactional(isolation = Isolation.READ_UNCOMMITTED)
	@Override
	public List<XrplDataNftDto> getNftOwners(FsePaymentRequest paymentRequest) {

		return xrplNftClient.getNftOwnersForIssuer(paymentRequest.getNftIssuingAddress(), Optional.ofNullable(paymentRequest.getNftTaxon()));
		
	}


}
