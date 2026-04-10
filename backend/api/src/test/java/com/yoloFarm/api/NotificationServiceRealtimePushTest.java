package com.yoloFarm.api;

import com.yoloFarm.api.entity.Notification;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.repository.NotificationRepository;
import com.yoloFarm.api.repository.UserRepository;
import com.yoloFarm.api.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceRealtimePushTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void createSystemNotificationShouldPushUnreadCountToUserQueue() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("farmer-a")
                .password("pwd")
                .email("farmer-a@yolo.test")
                .role(RoleEnum.FARMER)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.countUnreadByUserId(userId)).thenReturn(3L);

        notificationService.createSystemNotification(userId, "Device approved");

        verify(messagingTemplate).convertAndSendToUser(
                eq("farmer-a"),
                eq("/queue/notifications-unread"),
                eq(Map.of("unread_count", 3L)));
    }

    @Test
    void markAsReadShouldPushUpdatedUnreadCount() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("farmer-b")
                .password("pwd")
                .email("farmer-b@yolo.test")
                .role(RoleEnum.FARMER)
                .build();

        Notification notification = Notification.builder()
                .id(notificationId)
                .user(user)
                .message("Irrigation completed")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.countUnreadByUserId(userId)).thenReturn(1L);

        notificationService.markAsRead(notificationId, userId);

        assertEquals(true, notification.getIsRead());
        verify(messagingTemplate).convertAndSendToUser(
                eq("farmer-b"),
                eq("/queue/notifications-unread"),
                eq(Map.of("unread_count", 1L)));
    }
}
