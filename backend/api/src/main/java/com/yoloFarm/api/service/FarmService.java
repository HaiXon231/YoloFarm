package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.request.FarmCreateRequest;
import com.yoloFarm.api.dto.response.FarmResponse;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.exception.ConflictException;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.RuleRepository;
import com.yoloFarm.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FarmService {

    private final FarmRepository farmRepository;
    private final DeviceRepository deviceRepository;
    private final RuleRepository ruleRepository;
    private final UserRepository userRepository;

    @Transactional
    public FarmResponse createFarm(FarmCreateRequest request, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + ownerId));

        Farm farm = Farm.builder()
                .name(request.getName())
                .location(request.getLocation())
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

    public FarmResponse getFarmById(UUID farmId, UUID ownerId) {
        Farm farm = farmRepository.findByIdAndOwnerId(farmId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Farm not found with id: " + farmId));
        return mapToResponse(farm);
    }

    @Transactional
    public FarmResponse updateFarm(UUID farmId, UUID ownerId, FarmCreateRequest request) {
        Farm farm = farmRepository.findByIdAndOwnerId(farmId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Farm not found with id: " + farmId));
        farm.setName(request.getName());
        farm.setLocation(request.getLocation());
        return mapToResponse(farmRepository.save(farm));
    }

    @Transactional
    public void deleteFarm(UUID farmId, UUID ownerId) {
        Farm farm = farmRepository.findByIdAndOwnerId(farmId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Farm not found with id: " + farmId));

        long deviceCount = deviceRepository.countByFarmId(farmId);
        if (deviceCount > 0) {
            throw new ConflictException("Không thể xóa farm vì vẫn còn thiết bị liên kết");
        }

        long ruleCount = ruleRepository.countByFarmId(farmId);
        if (ruleCount > 0) {
            throw new ConflictException("Không thể xóa farm vì vẫn còn rule liên kết");
        }

        farmRepository.delete(farm);
    }

    private FarmResponse mapToResponse(Farm farm) {
        FarmResponse response = new FarmResponse();
        response.setId(farm.getId());
        response.setOwnerId(farm.getOwner().getId());
        response.setName(farm.getName());
        response.setLocation(farm.getLocation());
        response.setCreatedAt(farm.getCreatedAt());
        return response;
    }
}
