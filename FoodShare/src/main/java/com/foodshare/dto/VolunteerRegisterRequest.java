package com.foodshare.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VolunteerRegisterRequest {
    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    @NotNull(message = "Max capacity in kg is required")
    @Min(value = 1, message = "Capacity must be at least 1 kg")
    private Integer maxCapacityKg;

    public VolunteerRegisterRequest() {}

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Integer getMaxCapacityKg() { return maxCapacityKg; }
    public void setMaxCapacityKg(Integer maxCapacityKg) { this.maxCapacityKg = maxCapacityKg; }
}
