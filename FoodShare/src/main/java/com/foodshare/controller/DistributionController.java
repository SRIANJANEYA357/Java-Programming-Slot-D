package com.foodshare.controller;

import com.foodshare.dto.DistributionCampRequest;
import com.foodshare.dto.DistributionRecordResponse;
import com.foodshare.entity.User;
import com.foodshare.service.DistributionService;
import com.foodshare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distributions")
public class DistributionController {

    private final DistributionService distributionService;
    private final UserService userService;

    public DistributionController(DistributionService distributionService, UserService userService) {
        this.distributionService = distributionService;
        this.userService = userService;
    }

    @PostMapping("/camp")
    public ResponseEntity<DistributionRecordResponse> createCampDistribution(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DistributionCampRequest request) {
        User volunteer = userService.findByEmail(userDetails.getUsername());
        DistributionRecordResponse record = distributionService.createCampDistribution(request, volunteer);
        return new ResponseEntity<>(record, HttpStatus.CREATED);
    }

    @GetMapping("/history")
    public ResponseEntity<List<DistributionRecordResponse>> getAllHistory() {
        return ResponseEntity.ok(distributionService.getAllDistributions());
    }

    @GetMapping("/provider")
    public ResponseEntity<List<DistributionRecordResponse>> getProviderHistory(@AuthenticationPrincipal UserDetails userDetails) {
        User provider = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(distributionService.getDistributionsByProvider(provider.getId()));
    }

    @GetMapping("/recipient")
    public ResponseEntity<List<DistributionRecordResponse>> getRecipientHistory(@AuthenticationPrincipal UserDetails userDetails) {
        User recipient = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(distributionService.getDistributionsByRecipient(recipient.getId()));
    }

    @GetMapping("/volunteer")
    public ResponseEntity<List<DistributionRecordResponse>> getVolunteerHistory(@AuthenticationPrincipal UserDetails userDetails) {
        User volunteer = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(distributionService.getDistributionsByVolunteer(volunteer.getId()));
    }
}
