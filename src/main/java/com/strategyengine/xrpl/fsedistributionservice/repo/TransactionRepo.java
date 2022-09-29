package com.strategyengine.xrpl.fsedistributionservice.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.strategyengine.xrpl.fsedistributionservice.entity.TransactionEnt;

public interface TransactionRepo extends JpaRepository<TransactionEnt, Long>{


}
