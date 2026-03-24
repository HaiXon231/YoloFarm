package com.yoloFarm.api.dto.response;

import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private UUID id;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
