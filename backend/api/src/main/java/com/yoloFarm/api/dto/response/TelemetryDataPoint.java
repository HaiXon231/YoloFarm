package com.yoloFarm.api.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TelemetryDataPoint {
    private LocalDateTime time;
    private Float value;
}
