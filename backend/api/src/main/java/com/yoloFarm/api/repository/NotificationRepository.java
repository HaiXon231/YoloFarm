package com.yoloFarm.api.repository;

import com.yoloFarm.api.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
}
