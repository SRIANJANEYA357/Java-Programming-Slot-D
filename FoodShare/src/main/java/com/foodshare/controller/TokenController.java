package com.foodshare.controller;

import com.foodshare.dto.CollectionTokenResponse;
import com.foodshare.dto.TokenVerificationRequest;
import com.foodshare.dto.TokenVerificationResponse;
import com.foodshare.entity.User;
import com.foodshare.service.TokenService;
import com.foodshare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tokens")
public class TokenController {

    private final TokenService tokenService;
    private final UserService userService;

    public TokenController(TokenService tokenService, UserService userService) {
        this.tokenService = tokenService;
        this.userService = userService;
    }

    @GetMapping("/request/{requestId}")
    public ResponseEntity<CollectionTokenResponse> getTokenForRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User caller = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(tokenService.getByRequestIdAuthorized(requestId, caller));
    }

    @GetMapping("/code/{tokenCode}")
    public ResponseEntity<TokenVerificationResponse> getTokenDetails(@PathVariable String tokenCode) {
        return ResponseEntity.ok(tokenService.verifyToken(tokenCode));
    }

    @PostMapping("/verify")
    public ResponseEntity<TokenVerificationResponse> verifyToken(@Valid @RequestBody TokenVerificationRequest request) {
        return ResponseEntity.ok(tokenService.verifyToken(request.getTokenCode()));
    }

    @PostMapping("/confirm-collection")
    public ResponseEntity<TokenVerificationResponse> confirmCollection(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TokenVerificationRequest request) {
        User verifier = userService.findByEmail(userDetails.getUsername());
        TokenVerificationResponse response = tokenService.confirmCollection(request.getTokenCode(), verifier);
        return ResponseEntity.ok(response);
    }
}
