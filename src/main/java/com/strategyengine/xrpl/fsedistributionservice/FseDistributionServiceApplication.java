package com.strategyengine.xrpl.fsedistributionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.log4j.Log4j2;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Log4j2
@EnableScheduling
@SpringBootApplication
@EnableCaching
@EnableSwagger2
@EnableHystrix
public class FseDistributionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FseDistributionServiceApplication.class, args);
	}

}
