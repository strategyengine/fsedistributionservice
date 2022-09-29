package com.strategyengine.xrpl.fsedistributionservice.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.strategyengine.xrpl.fsedistributionservice.entity.ScammerAddressEnt;

public interface ScammerAddressRepo extends JpaRepository<ScammerAddressEnt, Long>{

	
}
