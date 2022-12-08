package com.strategyengine.xrpl.fsedistributionservice.client.xrp;

import java.util.List;
import java.util.Optional;

import com.strategyengine.xrpl.fsedistributionservice.model.XrplDataNftDto;

public interface XrplNftClient {

	List<XrplDataNftDto> getNftOwnersForIssuer(String issuer, Optional<Long> taxon);

}
