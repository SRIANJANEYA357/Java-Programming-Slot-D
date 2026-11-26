package com.foodshare.dto;

import java.time.LocalDateTime;

public class VolunteerResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String vehicleType;
    private String availabilityStatus;
    private Integer maxCapacityKg;
    private LocalDateTime createdAt;
    private UserSummary user;

    public static class UserSummary {
        private Long id;
        private String fullName;
        private String email;
        private String phone;

        public UserSummary() {}
        public UserSummary(Long id, String fullName, String email, String phone) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public VolunteerResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }

    public Integer getMaxCapacityKg() { return maxCapacityKg; }
    public void setMaxCapacityKg(Integer maxCapacityKg) { this.maxCapacityKg = maxCapacityKg; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public UserSummary getUser() { return user; }
    public void setUser(UserSummary user) { this.user = user; }
}
