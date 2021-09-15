package com.strategyengine.xrpl.fsedistributionservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xrpl.xrpl4j.client.XrplClient;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;

import okhttp3.HttpUrl;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * http://localhost:8080/swagger-ui.html
 * 
 * @author barry
 *
 */
@Configuration
@EnableSwagger2
public class FseDistributionServiceConfig {

	private static final String PROD_RIPPLE = "https://s1.ripple.com:51234/";
	private static final String TEST_RIPPLE = "https://s.altnet.rippletest.net:51234/";

	@Bean(name="xrplClient")
	public XrplClient xrplClient1() {
		final HttpUrl rippledUrl = HttpUrl.get(PROD_RIPPLE);

		XrplClient xrplClient = new XrplClient(rippledUrl);
		   	    
		return xrplClient;
	}
	
	@Bean
	public BigQuery bigQuery() {
		
	     return BigQueryOptions.getDefaultInstance().getService();

	}

}
