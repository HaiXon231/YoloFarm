package com.yoloFarm.api.dto.response;

import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
public class FarmResponse {
    private UUID id;
    private UUID ownerId;
    private String name;
    private String location;
    private LocalDateTime createdAt;
}
