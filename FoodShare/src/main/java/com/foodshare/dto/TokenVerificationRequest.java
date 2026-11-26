package com.foodshare.dto;

import jakarta.validation.constraints.NotBlank;

public class TokenVerificationRequest {
    @NotBlank(message = "Token code is required")
    private String tokenCode;

    public TokenVerificationRequest() {}

    public TokenVerificationRequest(String tokenCode) {
        this.tokenCode = tokenCode;
    }

    public String getTokenCode() { return tokenCode; }
    public void setTokenCode(String tokenCode) { this.tokenCode = tokenCode; }
}
