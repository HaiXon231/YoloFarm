package com.yoloFarm.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalFarmers;
    private long totalFarms;
    private long totalDevices;
    private long pendingRequests;
    private long activeDevices;
    
    // Infrastructure status
    private boolean apiStatus;
    private boolean mqttStatus;
    private boolean dbStatus;
}
