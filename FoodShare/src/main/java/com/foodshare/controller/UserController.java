package com.foodshare.controller;

import com.foodshare.dto.UserProfileDTO;
import com.foodshare.entity.Role;
import com.foodshare.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileDTO profile = userService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDTO> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                        @RequestBody UserProfileDTO updateDto) {
        UserProfileDTO updated = userService.updateProfile(userDetails.getUsername(), updateDto);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDTO> assignRole(@PathVariable Long userId,
                                                     @RequestParam Role role,
                                                     @AuthenticationPrincipal UserDetails adminUser) {
        String adminEmail = (adminUser != null) ? adminUser.getUsername() : null;
        return ResponseEntity.ok(userService.assignRole(userId, role, adminEmail));
    }
}
