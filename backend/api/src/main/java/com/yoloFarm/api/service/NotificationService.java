package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.response.NotificationResponse;
import com.yoloFarm.api.entity.Notification;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.repository.NotificationRepository;
import com.yoloFarm.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;

	public List<NotificationResponse> getNotifications(UUID userId, int page, int size) {
		int safePage = Math.max(page - 1, 0);
		int safeSize = Math.max(size, 1);
		return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(safePage, safeSize))
				.stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	@Transactional
	public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
		Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
				.orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + notificationId));
		notification.setIsRead(true);
		return mapToResponse(notificationRepository.save(notification));
	}

	@Transactional
	public void createSystemNotification(UUID userId, String message) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

		Notification notification = Notification.builder()
				.user(user)
				.message(message)
				.isRead(false)
				.createdAt(LocalDateTime.now())
				.build();
		notificationRepository.save(notification);
	}

	private NotificationResponse mapToResponse(Notification notification) {
		NotificationResponse response = new NotificationResponse();
		response.setId(notification.getId());
		response.setMessage(notification.getMessage());
		response.setIsRead(notification.getIsRead());
		response.setCreatedAt(notification.getCreatedAt());
		return response;
	}
}
