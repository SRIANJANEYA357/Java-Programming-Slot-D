package com.foodshare.dto;

import com.foodshare.entity.PriorityCategory;
import com.foodshare.entity.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FoodRequestResponse {
    private Long id;
    private Long listingId;
    private String listingTitle;
    private String listingImage;
    private String unit;
    private String locationAddress;
    private String locationCity;
    private Long recipientId;
    private String recipientName;
    private String recipientPhone;
    private Integer quantityRequested;
    private BigDecimal totalPrice;
    private Boolean isPriority;
    private PriorityCategory priorityCategory;
    private RequestStatus requestStatus;
    private String tokenCode;
    private String notes;
    private LocalDateTime createdAt;

    public FoodRequestResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }

    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }

    public String getListingImage() { return listingImage; }
    public void setListingImage(String listingImage) { this.listingImage = listingImage; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }

    public String getLocationCity() { return locationCity; }
    public void setLocationCity(String locationCity) { this.locationCity = locationCity; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public Integer getQuantityRequested() { return quantityRequested; }
    public void setQuantityRequested(Integer quantityRequested) { this.quantityRequested = quantityRequested; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public Boolean getIsPriority() { return isPriority; }
    public void setIsPriority(Boolean isPriority) { this.isPriority = isPriority; }

    public PriorityCategory getPriorityCategory() { return priorityCategory; }
    public void setPriorityCategory(PriorityCategory priorityCategory) { this.priorityCategory = priorityCategory; }

    public RequestStatus getRequestStatus() { return requestStatus; }
    public void setRequestStatus(RequestStatus requestStatus) { this.requestStatus = requestStatus; }

    public String getTokenCode() { return tokenCode; }
    public void setTokenCode(String tokenCode) { this.tokenCode = tokenCode; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public ListingSummary getListing() {
        return new ListingSummary(listingId, listingTitle, unit);
    }

    public RecipientSummary getRecipient() {
        return new RecipientSummary(recipientId, recipientName);
    }

    public static class ListingSummary {
        private final Long id;
        private final String title;
        private final String unit;

        public ListingSummary(Long id, String title, String unit) {
            this.id = id;
            this.title = title;
            this.unit = unit;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getUnit() { return unit; }
    }

    public static class RecipientSummary {
        private final Long id;
        private final String fullName;

        public RecipientSummary(Long id, String fullName) {
            this.id = id;
            this.fullName = fullName;
        }

        public Long getId() { return id; }
        public String getFullName() { return fullName; }
    }
}
