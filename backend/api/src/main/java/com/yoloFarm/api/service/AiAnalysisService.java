package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.response.AiAnalysisResult;
import com.yoloFarm.api.dto.response.AiLogResponse;
import com.yoloFarm.api.repository.FarmRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiAnalysisService {

	private final FarmRepository farmRepository;
	private final List<AiLogResponse> inMemoryLogs = new CopyOnWriteArrayList<>();

	@Transactional
	public AiAnalysisResult analyzeImage(UUID ownerId, UUID farmId, MultipartFile file, String analysisType) {
		validateFarmOwnership(ownerId, farmId);

		if (file == null || file.isEmpty()) {
			throw new IllegalStateException("File ảnh không được để trống");
		}

		String fileName = file.getOriginalFilename() == null ? "uploaded-image" : file.getOriginalFilename();

		AiAnalysisResult result = new AiAnalysisResult();
		result.setResultLabel("Mock result for " + analysisType);
		result.setConfidenceScore(0.80f);
		result.setImageUrl("/mock-storage/" + fileName);

		AiLogResponse log = new AiLogResponse();
		log.setId(UUID.randomUUID());
		log.setAnalysisType(analysisType);
		log.setResultLabel(result.getResultLabel());
		log.setConfidenceScore(result.getConfidenceScore());
		log.setImageUrl(result.getImageUrl());
		log.setAnalyzedAt(LocalDateTime.now());
		synchronized (inMemoryLogs) {
			inMemoryLogs.add(log);
		}

		return result;
	}

	public List<AiLogResponse> getLogs(UUID ownerId, UUID farmId) {
		validateFarmOwnership(ownerId, farmId);
		List<AiLogResponse> copy;
		synchronized (inMemoryLogs) {
			copy = new ArrayList<>(inMemoryLogs);
		}
		copy.sort(Comparator.comparing(AiLogResponse::getAnalyzedAt).reversed());
		return copy;
	}

	private void validateFarmOwnership(UUID ownerId, UUID farmId) {
		if (farmRepository.findByIdAndOwnerId(farmId, ownerId).isEmpty()) {
			throw new EntityNotFoundException("Farm not found with id: " + farmId);
		}
	}
}
