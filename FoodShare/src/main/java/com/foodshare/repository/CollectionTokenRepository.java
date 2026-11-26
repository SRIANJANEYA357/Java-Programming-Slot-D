package com.foodshare.repository;

import com.foodshare.entity.CollectionToken;
import com.foodshare.entity.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionTokenRepository extends JpaRepository<CollectionToken, Long> {
    Optional<CollectionToken> findByTokenCode(String tokenCode);
    Optional<CollectionToken> findByRequestId(Long requestId);
    boolean existsByTokenCode(String tokenCode);
    List<CollectionToken> findByStatus(TokenStatus status);
}
