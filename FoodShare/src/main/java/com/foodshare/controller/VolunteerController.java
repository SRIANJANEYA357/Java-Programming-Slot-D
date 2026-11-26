package com.foodshare.controller;

import com.foodshare.dto.VolunteerRegisterRequest;
import com.foodshare.dto.VolunteerResponse;
import com.foodshare.entity.User;
import com.foodshare.service.UserService;
import com.foodshare.service.VolunteerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/volunteers")
public class VolunteerController {

    private final VolunteerService volunteerService;
    private final UserService userService;

    public VolunteerController(VolunteerService volunteerService, UserService userService) {
        this.volunteerService = volunteerService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<VolunteerResponse> registerVolunteer(@AuthenticationPrincipal UserDetails userDetails,
                                                               @Valid @RequestBody VolunteerRegisterRequest request) {
        User user = userService.findByEmail(userDetails.getUsername());
        VolunteerResponse volunteer = volunteerService.registerVolunteer(user, request);
        return ResponseEntity.ok(volunteer);
    }

    @GetMapping("/me")
    public ResponseEntity<VolunteerResponse> getMyVolunteerProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(volunteerService.getVolunteerByUser(user));
    }

    @PutMapping("/availability")
    public ResponseEntity<VolunteerResponse> updateAvailability(@AuthenticationPrincipal UserDetails userDetails,
                                                                @RequestBody Map<String, String> body) {
        User user = userService.findByEmail(userDetails.getUsername());
        String status = body.getOrDefault("status", "AVAILABLE");
        return ResponseEntity.ok(volunteerService.updateAvailability(user, status));
    }

    @GetMapping("/available")
    public ResponseEntity<List<VolunteerResponse>> getAvailableVolunteers() {
        return ResponseEntity.ok(volunteerService.getAvailableVolunteers());
    }
}
