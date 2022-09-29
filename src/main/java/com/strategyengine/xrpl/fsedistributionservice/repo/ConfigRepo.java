package com.strategyengine.xrpl.fsedistributionservice.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.strategyengine.xrpl.fsedistributionservice.entity.ConfigEnt;

public interface ConfigRepo extends JpaRepository<ConfigEnt, Long>{


}
