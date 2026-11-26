package com.foodshare.dto;

import com.foodshare.entity.PriorityCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class FoodRequestDto {
    @NotNull(message = "Listing ID is required")
    private Long listingId;

    @NotNull(message = "Requested quantity is required")
    @Min(value = 1, message = "Must request at least 1 unit")
    private Integer quantityRequested;

    private Boolean isPriority = false;
    private PriorityCategory priorityCategory = PriorityCategory.REGULAR;
    private String notes;

    public FoodRequestDto() {}

    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }

    public Integer getQuantityRequested() { return quantityRequested; }
    public void setQuantityRequested(Integer quantityRequested) { this.quantityRequested = quantityRequested; }

    public Boolean getIsPriority() { return isPriority; }
    public void setIsPriority(Boolean isPriority) { this.isPriority = isPriority; }

    public PriorityCategory getPriorityCategory() { return priorityCategory; }
    public void setPriorityCategory(PriorityCategory priorityCategory) { this.priorityCategory = priorityCategory; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
