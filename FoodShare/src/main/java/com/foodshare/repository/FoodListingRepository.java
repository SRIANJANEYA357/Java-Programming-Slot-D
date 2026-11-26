package com.foodshare.repository;

import com.foodshare.entity.FoodListing;
import com.foodshare.entity.FoodType;
import com.foodshare.entity.ListingCategory;
import com.foodshare.entity.ListingStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FoodListingRepository extends JpaRepository<FoodListing, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT f FROM FoodListing f WHERE f.id = :id")
    Optional<FoodListing> findByIdForUpdate(@Param("id") Long id);

    List<FoodListing> findByProviderIdOrderByCreatedAtDesc(Long providerId);

    List<FoodListing> findByStatusAndExpiryTimeAfterOrderByExpiryTimeAsc(ListingStatus status, LocalDateTime now);

    @Query("SELECT f FROM FoodListing f WHERE f.status = 'AVAILABLE' AND f.expiryTime > :now " +
           "AND (:category IS NULL OR f.category = :category) " +
           "AND (:foodType IS NULL OR f.foodType = :foodType) " +
           "AND (:city IS NULL OR LOWER(f.locationCity) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND (:search IS NULL OR LOWER(f.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(f.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY f.expiryTime ASC")
    List<FoodListing> searchAvailableListings(
            @Param("search") String search,
            @Param("category") ListingCategory category,
            @Param("foodType") FoodType foodType,
            @Param("city") String city,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT f FROM FoodListing f WHERE f.status = 'AVAILABLE' AND f.expiryTime <= :now")
    List<FoodListing> findExpiredListings(@Param("now") LocalDateTime now);
}
