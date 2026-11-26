package com.foodshare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "volunteers")
public class Volunteer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 50)
    private String vehicleType = "TWO_WHEELER";

    @Column(length = 30)
    private String availabilityStatus = "AVAILABLE";

    private Integer maxCapacityKg = 25;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Volunteer() {}

    public Volunteer(User user, String vehicleType, Integer maxCapacityKg) {
        this.user = user;
        this.vehicleType = vehicleType;
        this.maxCapacityKg = maxCapacityKg;
        this.availabilityStatus = "AVAILABLE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }

    public Integer getMaxCapacityKg() { return maxCapacityKg; }
    public void setMaxCapacityKg(Integer maxCapacityKg) { this.maxCapacityKg = maxCapacityKg; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
