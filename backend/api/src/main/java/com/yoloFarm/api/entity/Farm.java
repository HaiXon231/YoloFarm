package com.yoloFarm.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "farms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Farm {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255)
    private String location;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "farm", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Device> devices;

    @OneToMany(mappedBy = "farm", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Rule> rules;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
