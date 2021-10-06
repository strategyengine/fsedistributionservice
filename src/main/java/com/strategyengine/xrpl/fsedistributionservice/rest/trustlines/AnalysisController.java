package com.strategyengine.xrpl.fsedistributionservice.rest.trustlines;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.service.AnalysisService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Api(tags = "XRPL Trustline analysis")
@RestController
public class AnalysisController {

	
	@VisibleForTesting
	@Autowired
	protected AnalysisService analysisService;
	

	@ApiOperation(value = "Find the addresses that have been paid by this address")
	@RequestMapping(value = "/analysis/paidaddresses/{classicAddress}", method = RequestMethod.GET)
	public Set<String> getLineage(
			@ApiParam(value = "Classic XRP address that sent the tokens. Example rnL2P...", required = true) @PathVariable("classicAddress") String classicAddress) {

		return analysisService.getPaidAddresses(classicAddress);

	}

	//NOT ready yet.  activation address not returning consinstently from xrplclient
//	@ApiOperation(value = "Find addresses with trustlines that activate many child addresses with the same trustline")
//	@RequestMapping(value = "/analysis/activations/{issuingAddress}/{currencyName}", method = RequestMethod.GET)
//	public Map<String, List<String>> getActivations(
//			@ApiParam(value = "Classic XRP address that issued the tokens. Example rnL2P...", required = true) @PathVariable("issuingAddress") String issuingAddress,
//			@ApiParam(value = "Currency name Example FSE", required = true) @PathVariable("currencyName") String currencyName,
//			@ApiParam(value = "OPTIONAL Minimum activations to return a parent. Default is 20 For example 10, will only return parent addresses that have activated 10 or more addresses containing this trustline", required = true) @RequestParam(value="minActivations", required=false, defaultValue = "20") int minActivations) {
//		
//		return analysisService.getActivations(issuingAddress, currencyName, minActivations);
//	}


}