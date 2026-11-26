package com.foodshare.dto;

import com.foodshare.entity.ProviderType;
import com.foodshare.entity.Role;

public class AuthResponse {
    private String token;
    private Long id;
    private String email;
    private String fullName;
    private Role role;
    private ProviderType providerType;
    private String organizationName;

    public AuthResponse() {}

    public AuthResponse(String token, Long id, String email, String fullName, Role role, ProviderType providerType, String organizationName) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.providerType = providerType;
        this.organizationName = organizationName;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public ProviderType getProviderType() { return providerType; }
    public void setProviderType(ProviderType providerType) { this.providerType = providerType; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
}
