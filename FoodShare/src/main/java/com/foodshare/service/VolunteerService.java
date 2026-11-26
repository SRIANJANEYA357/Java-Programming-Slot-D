package com.foodshare.service;

import com.foodshare.dto.VolunteerRegisterRequest;
import com.foodshare.dto.VolunteerResponse;
import com.foodshare.entity.Volunteer;
import com.foodshare.entity.User;
import com.foodshare.exception.ResourceNotFoundException;
import com.foodshare.repository.VolunteerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;

    public VolunteerService(VolunteerRepository volunteerRepository) {
        this.volunteerRepository = volunteerRepository;
    }

    @Transactional
    public VolunteerResponse registerVolunteer(User user, VolunteerRegisterRequest req) {
        Optional<Volunteer> existing = volunteerRepository.findByUserId(user.getId());
        Volunteer volunteer = existing.orElseGet(Volunteer::new);

        volunteer.setUser(user);
        volunteer.setVehicleType(req.getVehicleType());
        volunteer.setMaxCapacityKg(req.getMaxCapacityKg());
        volunteer.setAvailabilityStatus("AVAILABLE");

        Volunteer saved = volunteerRepository.save(volunteer);
        return mapToResponse(saved);
    }

    public VolunteerResponse getVolunteerByUser(User user) {
        Volunteer volunteer = volunteerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer details not registered yet."));
        return mapToResponse(volunteer);
    }

    @Transactional
    public VolunteerResponse updateAvailability(User user, String status) {
        Volunteer volunteer = volunteerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer details not registered yet."));
        volunteer.setAvailabilityStatus(status);
        Volunteer saved = volunteerRepository.save(volunteer);
        return mapToResponse(saved);
    }

    public List<VolunteerResponse> getAvailableVolunteers() {
        return volunteerRepository.findByAvailabilityStatus("AVAILABLE")
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public VolunteerResponse mapToResponse(Volunteer v) {
        VolunteerResponse res = new VolunteerResponse();
        res.setId(v.getId());
        res.setVehicleType(v.getVehicleType());
        res.setAvailabilityStatus(v.getAvailabilityStatus());
        res.setMaxCapacityKg(v.getMaxCapacityKg());
        res.setCreatedAt(v.getCreatedAt());

        if (v.getUser() != null) {
            res.setUserId(v.getUser().getId());
            res.setFullName(v.getUser().getFullName());
            res.setEmail(v.getUser().getEmail());
            res.setPhone(v.getUser().getPhone());
            res.setUser(new VolunteerResponse.UserSummary(
                    v.getUser().getId(),
                    v.getUser().getFullName(),
                    v.getUser().getEmail(),
                    v.getUser().getPhone()
            ));
        }
        return res;
    }
}
