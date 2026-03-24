package com.yoloFarm.api.dto.response;

import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
public class AiLogResponse {
    private UUID id;
    private String analysisType;
    private String resultLabel;
    private Float confidenceScore;
    private String imageUrl;
    private LocalDateTime analyzedAt;
}
