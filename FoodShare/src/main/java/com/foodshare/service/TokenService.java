package com.foodshare.service;

import com.foodshare.dto.CollectionTokenResponse;
import com.foodshare.dto.TokenVerificationResponse;
import com.foodshare.entity.*;
import com.foodshare.exception.FoodExpiredException;
import com.foodshare.exception.InvalidTokenException;
import com.foodshare.exception.ResourceNotFoundException;
import com.foodshare.exception.UnauthorizedException;
import com.foodshare.repository.CollectionTokenRepository;
import com.foodshare.repository.DistributionRecordRepository;
import com.foodshare.repository.FoodListingRepository;
import com.foodshare.repository.FoodRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class TokenService {

    private final CollectionTokenRepository tokenRepository;
    private final FoodRequestRepository requestRepository;
    private final DistributionRecordRepository distributionRecordRepository;
    private final FoodListingRepository listingRepository;

    private static final String TOKEN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public TokenService(CollectionTokenRepository tokenRepository,
                        FoodRequestRepository requestRepository,
                        DistributionRecordRepository distributionRecordRepository,
                        FoodListingRepository listingRepository) {
        this.tokenRepository = tokenRepository;
        this.requestRepository = requestRepository;
        this.distributionRecordRepository = distributionRecordRepository;
        this.listingRepository = listingRepository;
    }

    /**
     * Generates a cryptographically secure, unique collection token for a confirmed food request.
     * Format: FS-XXXX-XXXX
     */
    @Transactional
    public CollectionToken generateTokenForRequest(FoodRequest request) {
        // Prevent duplicate token for same request
        if (tokenRepository.findByRequestId(request.getId()).isPresent()) {
            return tokenRepository.findByRequestId(request.getId()).get();
        }

        String code;
        int attempts = 0;
        do {
            code = generateCode();
            attempts++;
            if (attempts > 50) {
                code = "FS-" + System.currentTimeMillis();
                break;
            }
        } while (tokenRepository.existsByTokenCode(code));

        CollectionToken token = new CollectionToken(request, code);
        return tokenRepository.save(token);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder("FS-");
        for (int i = 0; i < 4; i++) {
            sb.append(TOKEN_CHARS.charAt(RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        sb.append("-");
        for (int i = 0; i < 4; i++) {
            sb.append(TOKEN_CHARS.charAt(RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Verifies token authenticity without mutating state.
     */
    @Transactional(readOnly = true)
    public TokenVerificationResponse verifyToken(String tokenCode) {
        CollectionToken token = tokenRepository.findByTokenCode(tokenCode.trim().toUpperCase())
                .orElseThrow(() -> new InvalidTokenException("Invalid token code: " + tokenCode));

        TokenVerificationResponse res = new TokenVerificationResponse();
        res.setTokenCode(token.getTokenCode());
        res.setTokenStatus(token.getStatus());
        res.setGeneratedAt(token.getGeneratedAt());
        res.setVerifiedAt(token.getVerifiedAt());

        FoodRequest req = token.getRequest();
        res.setRequestId(req.getId());
        res.setQuantity(req.getQuantityRequested());
        res.setTotalPrice(req.getTotalPrice());

        FoodListing listing = req.getListing();
        res.setListingId(listing.getId());
        res.setListingTitle(listing.getTitle());
        res.setUnit(listing.getUnit());
        res.setProviderName(listing.getProvider().getFullName());

        User recipient = req.getRecipient();
        res.setRecipientName(recipient.getFullName());
        res.setRecipientPhone(recipient.getPhone());

        if (listing.isExpired() || (listing.getExpiryTime() != null && listing.getExpiryTime().isBefore(LocalDateTime.now()))) {
            res.setValid(false);
            res.setMessage("This food listing has expired on " + listing.getExpiryTime() + " and cannot be collected.");
        } else if (token.getStatus() == TokenStatus.ACTIVE) {
            res.setValid(true);
            res.setMessage("Token is ACTIVE and valid for food collection.");
        } else if (token.getStatus() == TokenStatus.VERIFIED) {
            res.setValid(false);
            res.setMessage("This token was ALREADY USED for collection at " + token.getVerifiedAt());
        } else {
            res.setValid(false);
            res.setMessage("Token status is " + token.getStatus() + ". Collection not permitted.");
        }

        return res;
    }

    /**
     * Atomically confirms food collection, burns the token (one-time use), and creates an audit record.
     */
    @Transactional
    public TokenVerificationResponse confirmCollection(String tokenCode, User verifier) {
        CollectionToken token = tokenRepository.findByTokenCode(tokenCode.trim().toUpperCase())
                .orElseThrow(() -> new InvalidTokenException("Invalid token code: " + tokenCode));

        if (token.getStatus() != TokenStatus.ACTIVE) {
            throw new InvalidTokenException("Token cannot be redeemed. Current status: " + token.getStatus());
        }

        FoodRequest req = token.getRequest();
        FoodListing listing = req.getListing();

        // Enforce food freshness: reject redemption of expired listings
        if (listing.isExpired() || (listing.getExpiryTime() != null && listing.getExpiryTime().isBefore(LocalDateTime.now()))) {
            throw new FoodExpiredException("This food listing expired on " + listing.getExpiryTime() + ". Distribution is rejected for food safety.");
        }

        // Security check: Only the provider or volunteer can confirm collection
        boolean isProvider = verifier.getId().equals(listing.getProvider().getId());
        boolean isVolunteerOrAdmin = verifier.getRole() == Role.ROLE_VOLUNTEER || verifier.getRole() == Role.ROLE_ADMIN;

        if (!isProvider && !isVolunteerOrAdmin) {
            throw new InvalidTokenException("You are not authorized to confirm this food collection.");
        }

        // Mark token as verified
        token.setStatus(TokenStatus.VERIFIED);
        token.setVerifiedAt(LocalDateTime.now());
        token.setVerifiedBy(verifier);
        tokenRepository.save(token);

        // Mark food request as collected
        req.setRequestStatus(RequestStatus.COLLECTED);
        requestRepository.save(req);

        // Create immutable DistributionRecord for transparency and audit
        DistributionRecord record = new DistributionRecord();
        record.setListing(listing);
        record.setRequest(req);
        record.setToken(token);
        record.setProvider(listing.getProvider());
        record.setRecipient(req.getRecipient());
        if (verifier.getRole() == Role.ROLE_VOLUNTEER) {
            record.setVolunteer(verifier);
            record.setDistributionType(DistributionType.VOLUNTEER_DELIVERY);
        } else {
            record.setDistributionType(DistributionType.DIRECT_PICKUP);
        }
        record.setQuantityDistributed(req.getQuantityRequested());
        record.setCollectedAt(LocalDateTime.now());
        record.setNotes("Successfully collected and verified using token " + token.getTokenCode());
        distributionRecordRepository.save(record);

        return verifyToken(token.getTokenCode());
    }

    public CollectionToken getByTokenCode(String tokenCode) {
        return tokenRepository.findByTokenCode(tokenCode)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found: " + tokenCode));
    }

    public CollectionToken getByRequestId(Long requestId) {
        return tokenRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("No token found for request ID: " + requestId));
    }

    @Transactional(readOnly = true)
    public CollectionTokenResponse getByRequestIdAuthorized(Long requestId, User caller) {
        CollectionToken token = tokenRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("No token found for request ID: " + requestId));

        FoodRequest req = token.getRequest();
        boolean isRecipient = req.getRecipient() != null && req.getRecipient().getId().equals(caller.getId());
        boolean isProvider = req.getListing() != null && req.getListing().getProvider() != null && req.getListing().getProvider().getId().equals(caller.getId());
        boolean isAdmin = caller.getRole() == Role.ROLE_ADMIN;

        if (!isRecipient && !isProvider && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to access this collection token.");
        }

        return mapToResponse(token);
    }

    public CollectionTokenResponse mapToResponse(CollectionToken t) {
        CollectionTokenResponse res = new CollectionTokenResponse();
        res.setId(t.getId());
        res.setRequestId(t.getRequest() != null ? t.getRequest().getId() : null);
        res.setTokenCode(t.getTokenCode());
        res.setStatus(t.getStatus());
        res.setGeneratedAt(t.getGeneratedAt());
        res.setVerifiedAt(t.getVerifiedAt());
        if (t.getRequest() != null && t.getRequest().getListing() != null) {
            res.setListingId(t.getRequest().getListing().getId());
            res.setListingTitle(t.getRequest().getListing().getTitle());
            res.setQuantity(t.getRequest().getQuantityRequested());
            res.setUnit(t.getRequest().getListing().getUnit());
        }
        return res;
    }
}
