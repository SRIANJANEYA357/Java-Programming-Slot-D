package com.foodshare.service;

import com.foodshare.dto.FoodListingRequest;
import com.foodshare.dto.FoodListingResponse;
import com.foodshare.entity.*;
import com.foodshare.exception.ResourceNotFoundException;
import com.foodshare.exception.UnauthorizedException;
import com.foodshare.repository.CollectionTokenRepository;
import com.foodshare.repository.FoodListingRepository;
import com.foodshare.repository.FoodRequestRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodListingService {

    private final FoodListingRepository listingRepository;
    private final FoodRequestRepository requestRepository;
    private final CollectionTokenRepository tokenRepository;

    public FoodListingService(FoodListingRepository listingRepository,
                              FoodRequestRepository requestRepository,
                              CollectionTokenRepository tokenRepository) {
        this.listingRepository = listingRepository;
        this.requestRepository = requestRepository;
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public FoodListingResponse createListing(FoodListingRequest req, User provider) {
        FoodListing listing = new FoodListing();
        listing.setProvider(provider);
        listing.setTitle(req.getTitle().trim());
        listing.setDescription(req.getDescription());
        listing.setCategory(req.getCategory());
        listing.setOriginalQuantity(req.getQuantity());
        listing.setRemainingQuantity(req.getQuantity());
        listing.setUnit(req.getUnit());
        listing.setFoodType(req.getFoodType());

        if (req.getFoodType() == FoodType.DISCOUNTED_SALE) {
            listing.setOriginalPrice(req.getOriginalPrice() != null ? req.getOriginalPrice() : BigDecimal.ZERO);
            listing.setDiscountedPrice(req.getDiscountedPrice() != null ? req.getDiscountedPrice() : BigDecimal.ZERO);
        } else {
            listing.setOriginalPrice(BigDecimal.ZERO);
            listing.setDiscountedPrice(BigDecimal.ZERO);
        }

        listing.setLocationAddress(req.getLocationAddress().trim());
        listing.setLocationCity(req.getLocationCity().trim());
        listing.setExpiryTime(req.getExpiryTime());
        listing.setImageUrl(req.getImageUrl());
        listing.setStatus(ListingStatus.AVAILABLE);

        FoodListing saved = listingRepository.save(listing);
        return mapToResponse(saved);
    }

    @Transactional
    public FoodListingResponse updateListing(Long listingId, FoodListingRequest req, User provider) {
        FoodListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + listingId));

        if (!listing.getProvider().getId().equals(provider.getId()) && provider.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("You do not own this listing.");
        }

        listing.setTitle(req.getTitle().trim());
        listing.setDescription(req.getDescription());
        listing.setCategory(req.getCategory());
        listing.setLocationAddress(req.getLocationAddress().trim());
        listing.setLocationCity(req.getLocationCity().trim());
        listing.setExpiryTime(req.getExpiryTime());
        if (req.getImageUrl() != null) listing.setImageUrl(req.getImageUrl());

        FoodListing updated = listingRepository.save(listing);
        return mapToResponse(updated);
    }

    @Transactional
    public void cancelListing(Long listingId, User provider) {
        FoodListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + listingId));

        if (!listing.getProvider().getId().equals(provider.getId()) && provider.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("You do not own this listing.");
        }

        listing.setStatus(ListingStatus.CANCELLED);
        listingRepository.save(listing);

        // Invalidate all active confirmed requests and tokens for this cancelled listing
        List<FoodRequest> activeRequests = requestRepository.findByListingIdOrderByCreatedAtDesc(listingId);
        for (FoodRequest req : activeRequests) {
            if (req.getRequestStatus() == RequestStatus.CONFIRMED || req.getRequestStatus() == RequestStatus.PENDING) {
                req.setRequestStatus(RequestStatus.CANCELLED);
                requestRepository.save(req);
                tokenRepository.findByRequestId(req.getId()).ifPresent(token -> {
                    if (token.getStatus() == TokenStatus.ACTIVE) {
                        token.setStatus(TokenStatus.CANCELLED);
                        tokenRepository.save(token);
                    }
                });
            }
        }
    }

    @Transactional(readOnly = true)
    public List<FoodListingResponse> searchAvailableListings(String search, ListingCategory category, FoodType foodType, String city) {
        LocalDateTime now = LocalDateTime.now();
        List<FoodListing> listings = listingRepository.searchAvailableListings(
                (search != null && !search.isBlank()) ? search.trim() : null,
                category,
                foodType,
                (city != null && !city.isBlank()) ? city.trim() : null,
                now
        );
        return listings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FoodListingResponse getListingById(Long id) {
        FoodListing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found: " + id));
        return mapToResponse(listing);
    }

    @Transactional(readOnly = true)
    public List<FoodListingResponse> getProviderListings(Long providerId) {
        return listingRepository.findByProviderIdOrderByCreatedAtDesc(providerId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Automatic maintenance task: updates expired listings to EXPIRED status.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoExpireListings() {
        LocalDateTime now = LocalDateTime.now();
        List<FoodListing> expired = listingRepository.findExpiredListings(now);
        for (FoodListing f : expired) {
            f.setStatus(ListingStatus.EXPIRED);
            listingRepository.save(f);
        }
    }

    public FoodListingResponse mapToResponse(FoodListing f) {
        FoodListingResponse res = new FoodListingResponse();
        res.setId(f.getId());
        res.setProviderId(f.getProvider().getId());
        res.setProviderName(f.getProvider().getFullName());
        res.setProviderType(f.getProvider().getProviderType());
        res.setTitle(f.getTitle());
        res.setDescription(f.getDescription());
        res.setCategory(f.getCategory());
        res.setOriginalQuantity(f.getOriginalQuantity());
        res.setRemainingQuantity(f.getRemainingQuantity());
        res.setUnit(f.getUnit());
        res.setFoodType(f.getFoodType());
        res.setOriginalPrice(f.getOriginalPrice());
        res.setDiscountedPrice(f.getDiscountedPrice());
        res.setLocationAddress(f.getLocationAddress());
        res.setLocationCity(f.getLocationCity());
        res.setExpiryTime(f.getExpiryTime());
        res.setImageUrl(f.getImageUrl());
        res.setStatus(f.getStatus());
        res.setExpired(f.isExpired());
        res.setCreatedAt(f.getCreatedAt());
        return res;
    }
}
