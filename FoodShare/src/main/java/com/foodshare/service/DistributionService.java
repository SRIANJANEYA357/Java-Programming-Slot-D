package com.foodshare.service;

import com.foodshare.dto.DistributionCampRequest;
import com.foodshare.dto.DistributionRecordResponse;
import com.foodshare.entity.*;
import com.foodshare.exception.FoodExpiredException;
import com.foodshare.exception.InsufficientQuantityException;
import com.foodshare.exception.ResourceNotFoundException;
import com.foodshare.repository.DistributionRecordRepository;
import com.foodshare.repository.FoodListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DistributionService {

    private final DistributionRecordRepository distributionRecordRepository;
    private final FoodListingRepository listingRepository;

    public DistributionService(DistributionRecordRepository distributionRecordRepository,
                               FoodListingRepository listingRepository) {
        this.distributionRecordRepository = distributionRecordRepository;
        this.listingRepository = listingRepository;
    }

    /**
     * Records an offline food distribution camp organized by a volunteer using surplus from a listing.
     */
    @Transactional
    public DistributionRecordResponse createCampDistribution(DistributionCampRequest req, User volunteer) {
        FoodListing listing = listingRepository.findByIdForUpdate(req.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + req.getListingId()));

        if (listing.getStatus() != ListingStatus.AVAILABLE || listing.isExpired()) {
            throw new FoodExpiredException("This food listing is expired or unavailable.");
        }

        if (listing.getRemainingQuantity() < req.getQuantity()) {
            throw new InsufficientQuantityException(
                    "Only " + listing.getRemainingQuantity() + " " + listing.getUnit() + " remaining."
            );
        }

        // Deduct inventory
        int remaining = listing.getRemainingQuantity() - req.getQuantity();
        listing.setRemainingQuantity(remaining);
        if (remaining == 0) {
            listing.setStatus(ListingStatus.DEPLETED);
        }
        listingRepository.save(listing);

        // Record camp distribution
        DistributionRecord record = new DistributionRecord();
        record.setListing(listing);
        record.setProvider(listing.getProvider());
        record.setRecipient(volunteer); // Camp represents aggregate community recipient
        record.setVolunteer(volunteer);
        record.setQuantityDistributed(req.getQuantity());
        record.setDistributionType(DistributionType.OFFLINE_CAMP);
        record.setCampName(req.getCampName().trim());
        record.setCampLocation(req.getCampLocation().trim());
        record.setCollectedAt(LocalDateTime.now());
        record.setNotes(req.getNotes() != null ? req.getNotes() : "Offline Community Camp Food Distribution");

        DistributionRecord saved = distributionRecordRepository.save(record);
        return mapToResponse(saved);
    }

    public List<DistributionRecordResponse> getAllDistributions() {
        return distributionRecordRepository.findAllByOrderByCollectedAtDesc()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<DistributionRecordResponse> getDistributionsByProvider(Long providerId) {
        return distributionRecordRepository.findByProviderIdOrderByCollectedAtDesc(providerId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<DistributionRecordResponse> getDistributionsByRecipient(Long recipientId) {
        return distributionRecordRepository.findByRecipientIdOrderByCollectedAtDesc(recipientId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<DistributionRecordResponse> getDistributionsByVolunteer(Long volunteerId) {
        return distributionRecordRepository.findByVolunteerIdOrderByCollectedAtDesc(volunteerId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public DistributionRecordResponse mapToResponse(DistributionRecord r) {
        DistributionRecordResponse res = new DistributionRecordResponse();
        res.setId(r.getId());
        res.setQuantityDistributed(r.getQuantityDistributed());
        res.setDistributionType(r.getDistributionType());
        res.setCampName(r.getCampName());
        res.setCampLocation(r.getCampLocation());
        res.setCollectedAt(r.getCollectedAt());
        res.setNotes(r.getNotes());

        if (r.getListing() != null) {
            res.setListing(new DistributionRecordResponse.ListingSummary(
                    r.getListing().getId(),
                    r.getListing().getTitle(),
                    r.getListing().getUnit()
            ));
        }
        if (r.getProvider() != null) {
            res.setProvider(new DistributionRecordResponse.UserSummary(
                    r.getProvider().getId(),
                    r.getProvider().getFullName()
            ));
        }
        if (r.getRecipient() != null) {
            res.setRecipient(new DistributionRecordResponse.UserSummary(
                    r.getRecipient().getId(),
                    r.getRecipient().getFullName()
            ));
        }
        if (r.getVolunteer() != null) {
            res.setVolunteer(new DistributionRecordResponse.UserSummary(
                    r.getVolunteer().getId(),
                    r.getVolunteer().getFullName()
            ));
        }
        return res;
    }
}
