package com.foodshare.dto;

import com.foodshare.entity.FoodType;
import com.foodshare.entity.ListingCategory;
import com.foodshare.entity.ListingStatus;
import com.foodshare.entity.ProviderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FoodListingResponse {
    private Long id;
    private Long providerId;
    private String providerName;
    private ProviderType providerType;
    private String title;
    private String description;
    private ListingCategory category;
    private Integer originalQuantity;
    private Integer remainingQuantity;
    private String unit;
    private FoodType foodType;
    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;
    private String locationAddress;
    private String locationCity;
    private LocalDateTime expiryTime;
    private String imageUrl;
    private ListingStatus status;
    private boolean isExpired;
    private LocalDateTime createdAt;

    public FoodListingResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public ProviderType getProviderType() { return providerType; }
    public void setProviderType(ProviderType providerType) { this.providerType = providerType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ListingCategory getCategory() { return category; }
    public void setCategory(ListingCategory category) { this.category = category; }

    public Integer getOriginalQuantity() { return originalQuantity; }
    public void setOriginalQuantity(Integer originalQuantity) { this.originalQuantity = originalQuantity; }

    public Integer getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(Integer remainingQuantity) { this.remainingQuantity = remainingQuantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public FoodType getFoodType() { return foodType; }
    public void setFoodType(FoodType foodType) { this.foodType = foodType; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public BigDecimal getDiscountedPrice() { return discountedPrice; }
    public void setDiscountedPrice(BigDecimal discountedPrice) { this.discountedPrice = discountedPrice; }

    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }

    public String getLocationCity() { return locationCity; }
    public void setLocationCity(String locationCity) { this.locationCity = locationCity; }

    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; }

    public boolean isExpired() { return isExpired; }
    public void setExpired(boolean expired) { isExpired = expired; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
