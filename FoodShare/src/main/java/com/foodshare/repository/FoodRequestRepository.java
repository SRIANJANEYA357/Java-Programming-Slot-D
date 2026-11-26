package com.foodshare.repository;

import com.foodshare.entity.FoodRequest;
import com.foodshare.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodRequestRepository extends JpaRepository<FoodRequest, Long> {
    List<FoodRequest> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    List<FoodRequest> findByListingIdOrderByCreatedAtDesc(Long listingId);

    @Query("SELECT r FROM FoodRequest r WHERE r.listing.provider.id = :providerId ORDER BY r.createdAt DESC")
    List<FoodRequest> findByProviderIdOrderByCreatedAtDesc(@Param("providerId") Long providerId);

    List<FoodRequest> findByRequestStatus(RequestStatus status);
}
