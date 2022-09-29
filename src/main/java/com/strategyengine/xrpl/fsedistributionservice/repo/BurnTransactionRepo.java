package com.strategyengine.xrpl.fsedistributionservice.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.strategyengine.xrpl.fsedistributionservice.entity.BurnTransactionEnt;

public interface BurnTransactionRepo extends JpaRepository<BurnTransactionEnt, Long>{

	
}
