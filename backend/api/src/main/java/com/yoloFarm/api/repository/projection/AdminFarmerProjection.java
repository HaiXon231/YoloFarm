package com.yoloFarm.api.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AdminFarmerProjection {
    UUID getId();

    String getUsername();

    String getEmail();

    LocalDateTime getCreatedAt();

    long getFarmCount();
}
