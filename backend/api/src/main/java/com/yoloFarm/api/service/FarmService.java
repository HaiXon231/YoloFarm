package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.request.FarmCreateRequest;
import com.yoloFarm.api.dto.response.FarmResponse;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;
    private final UserRepository userRepository;

    public FarmResponse createFarm(FarmCreateRequest request, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + ownerId));

        Farm farm = Farm.builder()
                .name(request.getName())
                .owner(owner)
                .build();
        
        farm = farmRepository.save(farm);
        return mapToResponse(farm);
    }

    public List<FarmResponse> getFarmsByUserId(UUID userId) {
        return farmRepository.findByOwnerId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private FarmResponse mapToResponse(Farm farm) {
        FarmResponse response = new FarmResponse();
        response.setId(farm.getId());
        response.setOwnerId(farm.getOwner().getId());
        response.setName(farm.getName());
        return response;
    }
}
