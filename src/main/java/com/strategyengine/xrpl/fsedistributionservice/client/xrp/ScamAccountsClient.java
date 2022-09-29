package com.strategyengine.xrpl.fsedistributionservice.client.xrp;

import java.util.List;

import com.strategyengine.xrpl.fsedistributionservice.model.ScammerAddress;

public interface ScamAccountsClient {

	/**
	API:
		https://api.xrplorer.com/custom/airdropfarmingaccounts
	
		Params:
		per_page (1-5000), default 1000
		page (int)
		Count and paging information in headers: Link, X-Total-Count, Pagination-Count, Pagination-Page, Pagination-Limit
	
		The results are always ordered by the order we have added them, so if you run it periodically, you can always just fetch from the end
	
		Pass api key as header ' x-api-key' on your GET request. Key: 680817af-faf6-4f11-b3bc-ba87217553c2
	*/
	List<ScammerAddress> getScammers(int page);

}
