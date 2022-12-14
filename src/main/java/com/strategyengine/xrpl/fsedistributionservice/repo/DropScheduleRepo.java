package com.strategyengine.xrpl.fsedistributionservice.repo;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.strategyengine.xrpl.fsedistributionservice.entity.DropScheduleEnt;

public interface DropScheduleRepo extends JpaRepository<DropScheduleEnt, Long>{

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query("update DropScheduleEnt p set p.lockUuid = :uuid where p.lockUuid is null and p.dropScheduleStatus = 'ACTIVE'")
	void updateUuid(String uuid);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query("update DropScheduleEnt p set p.lockUuid = null where p.lockUuid = :uuid")
	void removeUuid(String uuid);

	
}
