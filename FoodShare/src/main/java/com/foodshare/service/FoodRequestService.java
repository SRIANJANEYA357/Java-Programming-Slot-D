package com.foodshare.service;

import com.foodshare.dto.FoodRequestDto;
import com.foodshare.dto.FoodRequestResponse;
import com.foodshare.entity.*;
import com.foodshare.exception.FoodExpiredException;
import com.foodshare.exception.InsufficientQuantityException;
import com.foodshare.exception.ResourceNotFoundException;
import com.foodshare.exception.UnauthorizedException;
import com.foodshare.repository.CollectionTokenRepository;
import com.foodshare.repository.FoodListingRepository;
import com.foodshare.repository.FoodRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodRequestService {

    private final FoodListingRepository listingRepository;
    private final FoodRequestRepository requestRepository;
    private final CollectionTokenRepository tokenRepository;
    private final TokenService tokenService;

    public FoodRequestService(FoodListingRepository listingRepository,
                              FoodRequestRepository requestRepository,
                              CollectionTokenRepository tokenRepository,
                              TokenService tokenService) {
        this.listingRepository = listingRepository;
        this.requestRepository = requestRepository;
        this.tokenRepository = tokenRepository;
        this.tokenService = tokenService;
    }

    /**
     * Concurrency-safe request creation with row-level pessimistic write lock (SELECT FOR UPDATE).
     * Prevents overbooking even under high concurrent demand.
     */
    @Transactional
    public FoodRequestResponse createRequest(FoodRequestDto dto, User recipient) {
        // 1. Acquire pessimistic write lock on the listing
        FoodListing listing = listingRepository.findByIdForUpdate(dto.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + dto.getListingId()));

        // 2. Validate listing status and expiry
        if (listing.getStatus() == ListingStatus.DEPLETED || listing.getRemainingQuantity() <= 0) {
            throw new InsufficientQuantityException("This food listing has already been fully claimed.");
        }

        if (listing.getStatus() == ListingStatus.EXPIRED || listing.isExpired()) {
            listing.setStatus(ListingStatus.EXPIRED);
            listingRepository.save(listing);
            throw new FoodExpiredException("This food listing is expired.");
        }

        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            throw new InsufficientQuantityException("This food listing is no longer available.");
        }

        // Prevent self-claiming by providers
        if (listing.getProvider() != null && listing.getProvider().getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("Providers cannot request or claim their own food listings.");
        }

        // 3. Validate requested quantity against remaining inventory
        int requestedQty = dto.getQuantityRequested();
        if (requestedQty <= 0) {
            throw new IllegalArgumentException("Requested quantity must be at least 1.");
        }

        if (listing.getRemainingQuantity() < requestedQty) {
            throw new InsufficientQuantityException(
                    "Only " + listing.getRemainingQuantity() + " " + listing.getUnit() +
                    " remaining. You requested " + requestedQty + "."
            );
        }

        // 4. Calculate total price (Free vs Discounted)
        BigDecimal totalPrice = BigDecimal.ZERO;
        if (listing.getFoodType() == FoodType.DISCOUNTED_SALE && listing.getDiscountedPrice() != null) {
            totalPrice = listing.getDiscountedPrice().multiply(BigDecimal.valueOf(requestedQty));
        }

        // 5. Decrement inventory safely
        int updatedRemaining = listing.getRemainingQuantity() - requestedQty;
        listing.setRemainingQuantity(updatedRemaining);
        if (updatedRemaining == 0) {
            listing.setStatus(ListingStatus.DEPLETED);
        }
        listingRepository.save(listing);

        // 6. Create request entity
        FoodRequest request = new FoodRequest();
        request.setListing(listing);
        request.setRecipient(recipient);
        request.setQuantityRequested(requestedQty);
        request.setTotalPrice(totalPrice);
        request.setIsPriority(dto.getIsPriority() != null && dto.getIsPriority());
        request.setPriorityCategory(dto.getPriorityCategory() != null ? dto.getPriorityCategory() : PriorityCategory.REGULAR);
        request.setRequestStatus(RequestStatus.CONFIRMED);
        request.setNotes(dto.getNotes());

        FoodRequest savedRequest = requestRepository.save(request);

        // 7. Generate unique collection token immediately upon confirmation
        CollectionToken token = tokenService.generateTokenForRequest(savedRequest);

        return mapToResponse(savedRequest, token.getTokenCode());
    }

    /**
     * Cancel an active request and restore inventory to the food listing.
     */
    @Transactional
    public void cancelRequest(Long requestId, User user) {
        FoodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        if (!request.getRecipient().getId().equals(user.getId()) && user.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("You are not authorized to cancel this request.");
        }

        if (request.getRequestStatus() == RequestStatus.COLLECTED) {
            throw new IllegalStateException("Cannot cancel an already collected food request.");
        }

        if (request.getRequestStatus() == RequestStatus.CANCELLED) {
            return;
        }

        // Restore quantity with pessimistic lock
        FoodListing listing = listingRepository.findByIdForUpdate(request.getListing().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        listing.setRemainingQuantity(listing.getRemainingQuantity() + request.getQuantityRequested());
        if (listing.getStatus() == ListingStatus.DEPLETED && !listing.isExpired()) {
            listing.setStatus(ListingStatus.AVAILABLE);
        }
        listingRepository.save(listing);

        // Cancel token
        tokenRepository.findByRequestId(request.getId()).ifPresent(t -> {
            t.setStatus(TokenStatus.CANCELLED);
            tokenRepository.save(t);
        });

        request.setRequestStatus(RequestStatus.CANCELLED);
        requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<FoodRequestResponse> getRecipientRequests(Long recipientId) {
        return requestRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream()
                .map(r -> {
                    String tokenCode = tokenRepository.findByRequestId(r.getId())
                            .map(CollectionToken::getTokenCode)
                            .orElse(null);
                    return mapToResponse(r, tokenCode);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FoodRequestResponse> getProviderRequests(Long providerId) {
        return requestRepository.findByProviderIdOrderByCreatedAtDesc(providerId)
                .stream()
                .map(r -> {
                    String tokenCode = tokenRepository.findByRequestId(r.getId())
                            .map(CollectionToken::getTokenCode)
                            .orElse(null);
                    return mapToResponse(r, tokenCode);
                })
                .collect(Collectors.toList());
    }

    public FoodRequestResponse mapToResponse(FoodRequest r, String tokenCode) {
        FoodRequestResponse res = new FoodRequestResponse();
        res.setId(r.getId());
        res.setListingId(r.getListing().getId());
        res.setListingTitle(r.getListing().getTitle());
        res.setListingImage(r.getListing().getImageUrl());
        res.setUnit(r.getListing().getUnit());
        res.setLocationAddress(r.getListing().getLocationAddress());
        res.setLocationCity(r.getListing().getLocationCity());
        res.setRecipientId(r.getRecipient().getId());
        res.setRecipientName(r.getRecipient().getFullName());
        res.setRecipientPhone(r.getRecipient().getPhone());
        res.setQuantityRequested(r.getQuantityRequested());
        res.setTotalPrice(r.getTotalPrice());
        res.setIsPriority(r.getIsPriority());
        res.setPriorityCategory(r.getPriorityCategory());
        res.setRequestStatus(r.getRequestStatus());
        res.setTokenCode(tokenCode);
        res.setNotes(r.getNotes());
        res.setCreatedAt(r.getCreatedAt());
        return res;
    }
}
