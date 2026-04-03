package com.yoloFarm.api.repository;

import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.repository.projection.AdminFarmerProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    long countByRole(com.yoloFarm.api.enums.RoleEnum role);

    java.util.List<User> findByRole(com.yoloFarm.api.enums.RoleEnum role);

    @Query("""
            SELECT u.id AS id,
                   u.username AS username,
                   u.email AS email,
                   u.createdAt AS createdAt,
                   COUNT(f.id) AS farmCount
            FROM User u
            LEFT JOIN u.farms f
            WHERE u.role = :role
            GROUP BY u.id, u.username, u.email, u.createdAt
            ORDER BY u.createdAt DESC
            """)
    List<AdminFarmerProjection> findAdminFarmerSummaries(@Param("role") RoleEnum role);
}
