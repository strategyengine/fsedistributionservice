package com.strategyengine.xrpl.fsedistributionservice.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.SummaryResult;

public interface DropRecipientRepo extends JpaRepository<DropRecipientEnt, Long>{

	
	@Query( "select DISTINCT(o.address) from DropRecipientEnt o where dropRequestId in :paymentRequestIds and status = 'VERIFIED'" )
	List<String> findDistinctAddressesByPaymentRequestIds(@Param("paymentRequestIds") List<Long> paymentRequestIds);

	@Query( "select new com.strategyengine.xrpl.fsedistributionservice.entity.SummaryResult(o.dropRequestId, count(o.dropRequestId)) from DropRecipientEnt o where status = 'VERIFIED' and dropRequestId in (:paymentRequestIds) group by o.dropRequestId" )
	List<SummaryResult> countsByPaymentId(@Param("paymentRequestIds") List<Long> paymentRequestIds);
	
}
