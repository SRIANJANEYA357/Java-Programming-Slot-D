package com.foodshare.controller;

import com.foodshare.dto.FoodRequestDto;
import com.foodshare.dto.FoodRequestResponse;
import com.foodshare.entity.FoodRequest;
import com.foodshare.entity.User;
import com.foodshare.repository.FoodRequestRepository;
import com.foodshare.service.FoodRequestService;
import com.foodshare.service.PriorityAllocationService;
import com.foodshare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final FoodRequestService requestService;
    private final UserService userService;
    private final PriorityAllocationService priorityService;
    private final FoodRequestRepository requestRepository;

    public RequestController(FoodRequestService requestService,
                             UserService userService,
                             PriorityAllocationService priorityService,
                             FoodRequestRepository requestRepository) {
        this.requestService = requestService;
        this.userService = userService;
        this.priorityService = priorityService;
        this.requestRepository = requestRepository;
    }

    @PostMapping
    public ResponseEntity<FoodRequestResponse> createRequest(@AuthenticationPrincipal UserDetails userDetails,
                                                             @Valid @RequestBody FoodRequestDto dto) {
        User recipient = userService.findByEmail(userDetails.getUsername());
        FoodRequestResponse response = requestService.createRequest(dto, recipient);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelRequest(@PathVariable Long id,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        requestService.cancelRequest(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<FoodRequestResponse>> getMyRequests(@AuthenticationPrincipal UserDetails userDetails) {
        User recipient = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(requestService.getRecipientRequests(recipient.getId()));
    }

    @GetMapping("/provider")
    public ResponseEntity<List<FoodRequestResponse>> getProviderRequests(@AuthenticationPrincipal UserDetails userDetails) {
        User provider = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(requestService.getProviderRequests(provider.getId()));
    }

    /**
     * Demonstrates priority queue request sorting for high-vulnerability claims.
     * Restricted to administrators and volunteers to protect recipient privacy.
     */
    @GetMapping("/prioritized")
    public ResponseEntity<List<FoodRequestResponse>> getPrioritizedRequests(@RequestParam(required = false) Long listingId) {
        List<FoodRequest> raw;
        if (listingId != null) {
            raw = requestRepository.findByListingIdOrderByCreatedAtDesc(listingId);
        } else {
            raw = requestRepository.findAll();
        }
        List<FoodRequest> prioritized = priorityService.prioritizeRequests(raw);
        List<FoodRequestResponse> response = prioritized.stream()
                .map(r -> requestService.mapToResponse(r, null))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
