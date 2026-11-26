package com.foodshare.service;

import com.foodshare.config.JwtTokenProvider;
import com.foodshare.dto.AuthRequest;
import com.foodshare.dto.AuthResponse;
import com.foodshare.dto.RegisterRequest;
import com.foodshare.dto.UserProfileDTO;
import com.foodshare.entity.Role;
import com.foodshare.entity.User;
import com.foodshare.exception.ResourceNotFoundException;
import com.foodshare.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("Email is already registered: " + req.getEmail());
        }

        // Strict role validation: public registration is limited to RECIPIENT and PROVIDER only
        if (req.getRole() == null || (req.getRole() != Role.ROLE_RECIPIENT && req.getRole() != Role.ROLE_PROVIDER)) {
            throw new IllegalArgumentException("Public registration is only permitted for RECIPIENT and PROVIDER accounts. Privileged roles require administrative provisioning.");
        }

        User user = new User();
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName().trim());
        user.setPhone(req.getPhone().trim());
        user.setRole(req.getRole());

        if (req.getRole() == Role.ROLE_PROVIDER) {
            user.setProviderType(req.getProviderType());
            user.setOrganizationName(req.getOrganizationName());
        }

        user.setAddress(req.getAddress());
        user.setCity(req.getCity());

        User saved = userRepository.save(user);
        String token = tokenProvider.generateToken(saved);

        return new AuthResponse(
                token,
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getRole(),
                saved.getProviderType(),
                saved.getOrganizationName()
        );
    }

    public AuthResponse login(AuthRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, req.getPassword())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        String token = tokenProvider.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getProviderType(),
                user.getOrganizationName()
        );
    }

    public UserProfileDTO getProfile(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToProfile(user);
    }

    @Transactional
    public UserProfileDTO updateProfile(String email, UserProfileDTO updateDto) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (updateDto.getFullName() != null) user.setFullName(updateDto.getFullName().trim());
        if (updateDto.getPhone() != null) user.setPhone(updateDto.getPhone().trim());
        if (updateDto.getOrganizationName() != null) user.setOrganizationName(updateDto.getOrganizationName().trim());
        if (updateDto.getAddress() != null) user.setAddress(updateDto.getAddress().trim());
        if (updateDto.getCity() != null) user.setCity(updateDto.getCity().trim());

        User updated = userRepository.save(user);
        return mapToProfile(updated);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public List<UserProfileDTO> getAllUsers() {
        return userRepository.findAllByOrderByIdDesc()
                .stream()
                .map(this::mapToProfile)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserProfileDTO assignRole(Long userId, Role newRole) {
        return assignRole(userId, newRole, null);
    }

    @Transactional
    public UserProfileDTO assignRole(Long userId, Role newRole, String currentAdminEmail) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }

        if (newRole == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }

        if (newRole != Role.ROLE_RECIPIENT && newRole != Role.ROLE_PROVIDER && newRole != Role.ROLE_VOLUNTEER) {
            throw new IllegalArgumentException("Invalid role assignment: " + newRole + ". Permitted roles for assignment are ROLE_RECIPIENT, ROLE_PROVIDER, and ROLE_VOLUNTEER.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (currentAdminEmail != null && currentAdminEmail.trim().equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("Administrators cannot modify their own role.");
        }

        user.setRole(newRole);
        User saved = userRepository.save(user);
        return mapToProfile(saved);
    }

    private UserProfileDTO mapToProfile(User user) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setProviderType(user.getProviderType());
        dto.setOrganizationName(user.getOrganizationName());
        dto.setAddress(user.getAddress());
        dto.setCity(user.getCity());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
