package com.strategyengine.xrpl.fsedistributionservice;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Predicate;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * http://localhost:8080/swagger-ui.html
 * @author barry
 *
 */
@Configuration
public class SwaggerConfig {

	@Bean
	public Docket postsApi() {
		return new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo())
		        .select()
		        .apis(RequestHandlerSelectors.basePackage("com.strategyengine.xrpl.fsedistributionservice.rest.trustlines"))             
		          .paths(PathSelectors.any()).build();
	}

	private Predicate<String> postPaths() {
		return or(regex("/api/posts.*"), regex("/api/fsedistributionservice.*"));
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder().title("XRPL Issued Token Airdrop API - BETA")
				.description("API methods to help with issued token airdrops.  It is not recommended to use this service on a server you do not own.  "
						+ "If you must, then use a temp XRP address for any operation that requires you to input a private key.  "
						+ "Move just the required amounts to the temp address.")
				.license("Apache License v2.0")
				.version("1.0").build();
	}

}
