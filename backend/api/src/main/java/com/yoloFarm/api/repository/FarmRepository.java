package com.yoloFarm.api.repository;

import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.repository.projection.AdminFarmProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FarmRepository extends JpaRepository<Farm, UUID> {
    List<Farm> findByOwnerId(UUID ownerId);

    java.util.Optional<Farm> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    @Query("""
            SELECT f.id AS id,
                   f.name AS name,
                   f.location AS location,
                   o.username AS ownerName,
                   o.email AS ownerEmail,
                   f.createdAt AS createdAt,
                   COUNT(d.id) AS deviceCount
            FROM Farm f
            JOIN f.owner o
            LEFT JOIN f.devices d
            GROUP BY f.id, f.name, f.location, o.username, o.email, f.createdAt
            ORDER BY f.createdAt DESC
            """)
    List<AdminFarmProjection> findAdminFarmSummaries();
}
