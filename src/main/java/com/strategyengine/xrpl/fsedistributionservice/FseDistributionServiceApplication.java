package com.strategyengine.xrpl.fsedistributionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.log4j.Log4j2;

@Log4j2
@EnableScheduling
@SpringBootApplication
@EnableCaching
public class FseDistributionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FseDistributionServiceApplication.class, args);
	}

}
