package com.yoloFarm.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import com.yoloFarm.api.enums.RuleTypeEnum;
import com.yoloFarm.api.enums.ActionCommandEnum;

@Entity
@Table(name = "rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleTypeEnum ruleType;

    @Column(name = "rule_name", length = 100, nullable = false)
    private String ruleName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trigger_device_id")
    private Device triggerDevice;

    @Column(length = 5)
    private String operator;

    private Float thresholdValue;

    @Column(length = 100)
    private String cronExpression;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_device_id", nullable = false)
    private Device actionDevice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionCommandEnum actionCommand;

    @Column(nullable = false)
    private Boolean isActive;
}
