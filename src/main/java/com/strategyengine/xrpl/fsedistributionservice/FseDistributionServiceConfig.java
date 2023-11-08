package com.strategyengine.xrpl.fsedistributionservice;

import java.util.Properties;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestTemplate;
import org.xrpl.xrpl4j.client.XrplClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;

/**
 * http://localhost:8080/swagger-ui.html
 * 
 * @author barry
 *
 */
@Log4j2
@Configuration
public class FseDistributionServiceConfig {

//	private static final String PROD_RIPPLE = "https://s2.ripple.com:51234/";//https://xrplcluster.com///https://s1.ripple.com:51234/
//	private static final String PROD_RIPPLE = "https://xrpl.ws/";

	private static final String TEST_RIPPLE = "https://s.altnet.rippletest.net:51234/";

	@Value("${fsedistributionservice.version}")
	private String version;
	

	@Value("${fsedistributionservice.email.password}")
	private String emailPassword;

	@Value("${fsedistributionservice.email.address}")
	private String emailAddress;
	
	@Bean
	public JavaMailSender getJavaMailSender() {
	    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
	    mailSender.setHost("smtp.gmail.com");
	    mailSender.setPort(587);
	    
	    mailSender.setUsername(emailAddress);
	    mailSender.setPassword(emailPassword);
	    
	    Properties props = mailSender.getJavaMailProperties();
	    props.put("mail.transport.protocol", "smtp");
	    props.put("mail.smtp.auth", "true");
	    props.put("mail.smtp.starttls.enable", "true");
	    props.put("mail.debug", "true");
	    
	    return mailSender;
	}
	
	//testnet - https://hooks-testnet-v3.xrpl-labs.com
	@Bean(name="xahauClient")
	public XrplClient xahauClient() throws Exception {
		final HttpUrl url = HttpUrl.get("https://hooks-testnet-v3.xrpl-labs.com/");
		XrplClient client = null;
	
		client = fetchWithRetry(url, 0);
		
		return client;
	}
	

	@Bean(name = "xrplClient1")
	public XrplClient xrplClient1() throws Exception {
		log.info("LOADED VERSION " + version);
		log.info("ENV getenv " + System.getenv("ENV"));
		final HttpUrl rippledUrl = HttpUrl.get("https://s1.ripple.com:51234/");
		XrplClient xrplClient = null;
	
		xrplClient = fetchWithRetry(rippledUrl, 0);
		
		return xrplClient;
	}

	private XrplClient fetchWithRetry(HttpUrl rippledUrl, int i) throws InterruptedException {
		try {
			return new XrplClient(rippledUrl);
		}catch(Exception e) {
			if(i<15) {
				i++;
				Thread.sleep(500);
				return fetchWithRetry(rippledUrl, i);
			}
			throw e;
		}
	}

	@Bean(name = "xrplClient2")
	public XrplClient xrplClient2() throws Exception {
		final HttpUrl rippledUrl = HttpUrl.get("https://s2.ripple.com:51234/");

		XrplClient xrplClient = fetchWithRetry(rippledUrl, 0);


		return xrplClient;
	}

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();

		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		return objectMapper;
	}

	@Bean("scammerLookupRestTemplate")
	public RestTemplate scammerLookupRestTemplate() {
		return new RestTemplate(getClientHttpRequestFactory());
	}

	@Bean("globalIsServiceRestTemplate")
	public RestTemplate globalIsServiceRestTemplate() {
		return new RestTemplate(getClientHttpRequestFactory());
	}

	@Bean(name = "xrpScanRestTemplate")
	public RestTemplate xrpScanRestTemplate() {
		return new RestTemplate(getClientHttpRequestFactory());
	}
	
	@Bean(name = "xrplNftRestTemplate")
	public RestTemplate xrplNftRestTemplate() {
		return new RestTemplate(getClientHttpRequestFactory());
	}
	
	

	private ClientHttpRequestFactory getClientHttpRequestFactory() {
		int timeout = 60000;
		RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout).setConnectionRequestTimeout(timeout)
				.setSocketTimeout(timeout).build();
		CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
		return new HttpComponentsClientHttpRequestFactory(client);
	}

}
