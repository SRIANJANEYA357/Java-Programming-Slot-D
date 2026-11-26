package com.foodshare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "collection_tokens")
public class CollectionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private FoodRequest request;

    @Column(nullable = false, unique = true, length = 50)
    private String tokenCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TokenStatus status = TokenStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_id")
    private User verifiedBy;

    public CollectionToken() {}

    public CollectionToken(FoodRequest request, String tokenCode) {
        this.request = request;
        this.tokenCode = tokenCode;
        this.status = TokenStatus.ACTIVE;
        this.generatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FoodRequest getRequest() { return request; }
    public void setRequest(FoodRequest request) { this.request = request; }

    public String getTokenCode() { return tokenCode; }
    public void setTokenCode(String tokenCode) { this.tokenCode = tokenCode; }

    public TokenStatus getStatus() { return status; }
    public void setStatus(TokenStatus status) { this.status = status; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public User getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(User verifiedBy) { this.verifiedBy = verifiedBy; }
}
