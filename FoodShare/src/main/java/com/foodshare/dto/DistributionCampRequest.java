package com.foodshare.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DistributionCampRequest {
    @NotNull(message = "Listing ID is required")
    private Long listingId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Camp name is required")
    private String campName;

    @NotBlank(message = "Camp location is required")
    private String campLocation;

    private String notes;

    public DistributionCampRequest() {}

    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getCampName() { return campName; }
    public void setCampName(String campName) { this.campName = campName; }

    public String getCampLocation() { return campLocation; }
    public void setCampLocation(String campLocation) { this.campLocation = campLocation; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
