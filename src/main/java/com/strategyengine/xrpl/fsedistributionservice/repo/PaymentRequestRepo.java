package com.strategyengine.xrpl.fsedistributionservice.repo;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRequestStatus;

public interface PaymentRequestRepo extends JpaRepository<PaymentRequestEnt, Long>{

	//this could update multiple rows
	@Transactional
	@Modifying(clearAutomatically = true)
	@Query("update PaymentRequestEnt p set p.lockUuid = :uuid where p.lockUuid is null and status in ('QUEUED', 'IN_PROGRESS')")
	void updateUuid(String uuid);

	@Transactional	
	@Modifying(clearAutomatically = true)
	@Query("update PaymentRequestEnt p set p.lockUuid = null where p.lockUuid = :uuid")
	void resetNullUuidByUuid(String uuid);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query("update PaymentRequestEnt p set p.status = 'QUEUED' where p.lockUuid = :uuid")
	void resetStatusByUuid(String uuid);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query("select p.id from PaymentRequestEnt p where p.status = 'REJECTED' and p.id= :id")
	Optional<Long> isRejected(Long id);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query("select p from PaymentRequestEnt p where p.status = :status and p.createDate >= :created")
	List<PaymentRequestEnt> findByStatusAfterDate(DropRequestStatus status, Date created);
	
	@Transactional
	@Modifying(clearAutomatically = true)
	@Query("select p from PaymentRequestEnt p where p.status in ('IN_PROGRESS', 'QUEUED', 'POPULATING_ADDRESSES', 'PENDING_REVIEW')")
	Collection<PaymentRequestEnt> findActive();

	
}
