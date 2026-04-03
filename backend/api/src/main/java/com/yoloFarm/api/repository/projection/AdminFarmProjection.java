package com.yoloFarm.api.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AdminFarmProjection {
    UUID getId();

    String getName();

    String getLocation();

    String getOwnerName();

    String getOwnerEmail();

    LocalDateTime getCreatedAt();

    long getDeviceCount();
}
