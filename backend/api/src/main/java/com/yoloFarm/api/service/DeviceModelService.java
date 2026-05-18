package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.request.DeviceModelRequest;
import com.yoloFarm.api.dto.response.DeviceModelResponse;
import com.yoloFarm.api.entity.DeviceModel;
import com.yoloFarm.api.repository.DeviceModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceModelService {

	private final DeviceModelRepository deviceModelRepository;

	public List<DeviceModelResponse> getDeviceModels() {
		return deviceModelRepository.findAll()
				.stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	@Transactional
	public DeviceModelResponse createDeviceModel(DeviceModelRequest request) {
		validateRange(request.getMinValue(), request.getMaxValue());

		DeviceModel model = DeviceModel.builder()
				.modelName(request.getModelName())
				.deviceType(request.getDeviceType())
				.metricType(request.getMetricType())
				.manufacturer(request.getManufacturer())
				.displayUnit(request.getDisplayUnit())
				.minValue(request.getMinValue())
				.maxValue(request.getMaxValue())
				.modelDescription(request.getModelDescription())
				.referenceUrl(request.getReferenceUrl())
				.build();

		return mapToResponse(deviceModelRepository.save(model));
	}

	private void validateRange(Float minValue, Float maxValue) {
		if (minValue != null && maxValue != null && minValue >= maxValue) {
			throw new IllegalArgumentException("Model min_value phải nhỏ hơn max_value");
		}
	}

	private DeviceModelResponse mapToResponse(DeviceModel model) {
		DeviceModelResponse response = new DeviceModelResponse();
		response.setId(model.getId());
		response.setModelName(model.getModelName());
		response.setDeviceType(model.getDeviceType());
		response.setMetricType(model.getMetricType());
		response.setManufacturer(model.getManufacturer());
		response.setDisplayUnit(model.getDisplayUnit());
		response.setMinValue(model.getMinValue());
		response.setMaxValue(model.getMaxValue());
		response.setModelDescription(model.getModelDescription());
		response.setReferenceUrl(model.getReferenceUrl());
		return response;
	}
}
