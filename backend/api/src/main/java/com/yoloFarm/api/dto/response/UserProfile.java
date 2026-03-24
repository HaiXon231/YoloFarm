package com.yoloFarm.api.dto.response;

import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
public class UserProfile {
    private UUID id;
    private String username;
    private String email;
    private String role;
    private LocalDateTime createdAt;
}
