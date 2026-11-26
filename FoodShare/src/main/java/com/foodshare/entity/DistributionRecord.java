package com.foodshare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "distribution_records")
public class DistributionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "listing_id", nullable = false)
    private FoodListing listing;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "request_id")
    private FoodRequest request;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "token_id")
    private CollectionToken token;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "volunteer_id")
    private User volunteer;

    @Column(nullable = false)
    private Integer quantityDistributed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DistributionType distributionType;

    @Column(length = 150)
    private String campName;

    @Column(length = 255)
    private String campLocation;

    @Column(nullable = false)
    private LocalDateTime collectedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String notes;

    public DistributionRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FoodListing getListing() { return listing; }
    public void setListing(FoodListing listing) { this.listing = listing; }

    public FoodRequest getRequest() { return request; }
    public void setRequest(FoodRequest request) { this.request = request; }

    public CollectionToken getToken() { return token; }
    public void setToken(CollectionToken token) { this.token = token; }

    public User getProvider() { return provider; }
    public void setProvider(User provider) { this.provider = provider; }

    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }

    public User getVolunteer() { return volunteer; }
    public void setVolunteer(User volunteer) { this.volunteer = volunteer; }

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
}
