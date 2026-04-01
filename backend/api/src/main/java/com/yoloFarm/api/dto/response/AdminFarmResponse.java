package com.yoloFarm.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFarmResponse {
    private UUID id;
    private String name;
    private String location;
    private String ownerName;
    private String ownerEmail;
    private LocalDateTime createdAt;
    private long deviceCount;
}
