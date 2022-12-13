package com.strategyengine.xrpl.fsedistributionservice.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.strategyengine.xrpl.fsedistributionservice.entity.DropScheduleRunEnt;

public interface DropScheduleRunRepo extends JpaRepository<DropScheduleRunEnt, Long>{

	
}
