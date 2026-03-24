package com.yoloFarm.api.dto.response;

import lombok.Data;

@Data
public class AiAnalysisResult {
    private String resultLabel;
    private Float confidenceScore;
    private String imageUrl;
}
