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
public class AdminFarmerResponse {
    private UUID id;
    private String username;
    private String email;
    private LocalDateTime createdAt;
    private long farmCount;
}
