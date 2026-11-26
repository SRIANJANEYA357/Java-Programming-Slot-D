package com.foodshare.dto;

import com.foodshare.entity.DistributionType;
import java.time.LocalDateTime;

public class DistributionRecordResponse {
    private Long id;
    private Integer quantityDistributed;
    private DistributionType distributionType;
    private String campName;
    private String campLocation;
    private LocalDateTime collectedAt;
    private String notes;

    private ListingSummary listing;
    private UserSummary provider;
    private UserSummary recipient;
    private UserSummary volunteer;

    public static class ListingSummary {
        private Long id;
        private String title;
        private String unit;

        public ListingSummary() {}
        public ListingSummary(Long id, String title, String unit) {
            this.id = id;
            this.title = title;
            this.unit = unit;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }

    public static class UserSummary {
        private Long id;
        private String fullName;

        public UserSummary() {}
        public UserSummary(Long id, String fullName) {
            this.id = id;
            this.fullName = fullName;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
    }

    public DistributionRecordResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getQuantityDistributed() { return quantityDistributed; }
    public void setQuantityDistributed(Integer quantityDistributed) { this.quantityDistributed = quantityDistributed; }

    public DistributionType getDistributionType() { return distributionType; }
    public void setDistributionType(DistributionType distributionType) { this.distributionType = distributionType; }

    public String getCampName() { return campName; }
    public void setCampName(String campName) { this.campName = campName; }

    public String getCampLocation() { return campLocation; }
    public void setCampLocation(String campLocation) { this.campLocation = campLocation; }

    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public ListingSummary getListing() { return listing; }
    public void setListing(ListingSummary listing) { this.listing = listing; }

    public UserSummary getProvider() { return provider; }
    public void setProvider(UserSummary provider) { this.provider = provider; }

    public UserSummary getRecipient() { return recipient; }
    public void setRecipient(UserSummary recipient) { this.recipient = recipient; }

    public UserSummary getVolunteer() { return volunteer; }
    public void setVolunteer(UserSummary volunteer) { this.volunteer = volunteer; }
}
