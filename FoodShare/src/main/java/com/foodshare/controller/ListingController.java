package com.foodshare.controller;

import com.foodshare.dto.FoodListingRequest;
import com.foodshare.dto.FoodListingResponse;
import com.foodshare.entity.FoodType;
import com.foodshare.entity.ListingCategory;
import com.foodshare.entity.User;
import com.foodshare.service.FoodListingService;
import com.foodshare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final FoodListingService listingService;
    private final UserService userService;

    public ListingController(FoodListingService listingService, UserService userService) {
        this.listingService = listingService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<FoodListingResponse> createListing(@AuthenticationPrincipal UserDetails userDetails,
                                                             @Valid @RequestBody FoodListingRequest request) {
        User provider = userService.findByEmail(userDetails.getUsername());
        FoodListingResponse response = listingService.createListing(request, provider);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FoodListingResponse> updateListing(@PathVariable Long id,
                                                             @AuthenticationPrincipal UserDetails userDetails,
                                                             @Valid @RequestBody FoodListingRequest request) {
        User provider = userService.findByEmail(userDetails.getUsername());
        FoodListingResponse response = listingService.updateListing(id, request, provider);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelListing(@PathVariable Long id,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        User provider = userService.findByEmail(userDetails.getUsername());
        listingService.cancelListing(id, provider);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available")
    public ResponseEntity<List<FoodListingResponse>> getAvailable(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ListingCategory category,
            @RequestParam(required = false) FoodType foodType,
            @RequestParam(required = false) String city) {
        List<FoodListingResponse> listings = listingService.searchAvailableListings(search, category, foodType, city);
        return ResponseEntity.ok(listings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodListingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.getListingById(id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<FoodListingResponse>> getMyListings(@AuthenticationPrincipal UserDetails userDetails) {
        User provider = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(listingService.getProviderListings(provider.getId()));
    }
}
