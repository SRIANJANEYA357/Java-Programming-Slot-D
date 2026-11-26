package com.foodshare.repository;

import com.foodshare.entity.DistributionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistributionRecordRepository extends JpaRepository<DistributionRecord, Long> {
    List<DistributionRecord> findAllByOrderByCollectedAtDesc();
    List<DistributionRecord> findByProviderIdOrderByCollectedAtDesc(Long providerId);
    List<DistributionRecord> findByRecipientIdOrderByCollectedAtDesc(Long recipientId);
    List<DistributionRecord> findByVolunteerIdOrderByCollectedAtDesc(Long volunteerId);
}
