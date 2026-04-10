package com.yoloFarm.api.service;

import com.yoloFarm.api.entity.Rule;
import com.yoloFarm.api.enums.RuleTypeEnum;
import com.yoloFarm.api.repository.RuleRepository;
import com.yoloFarm.api.service.automation.AutomationRuntimeStateService;
import com.yoloFarm.api.service.strategy.IrrigationContext;
import com.yoloFarm.api.service.strategy.ScheduledStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleSchedulerService {

    private final RuleRepository ruleRepository;
    private final IrrigationContext irrigationContext;
    private final ScheduledStrategy scheduledStrategy;
    private final NotificationService notificationService;
    private final AutomationRuntimeStateService automationRuntimeStateService;
    private final Clock clock;

    /**
     * Chạy mỗi phút (giây 0) để kiểm tra các luật theo lịch trình.
     */
    @Scheduled(cron = "0 * * * * *")
    public void checkScheduledRules() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("RuleScheduler: Kiểm tra các luật SCHEDULE lúc {}", now);

        List<Rule> activeScheduledRules = ruleRepository
                .findActiveScheduledRulesWithAssociations(RuleTypeEnum.SCHEDULE);

        for (Rule rule : activeScheduledRules) {
            String cron = rule.getCronExpression();
            if (cron == null || cron.isBlank())
                continue;

            String[] parts = cron.trim().split("\\s+");
            if (parts.length == 5) {
                cron = "0 " + cron.trim();
            }

            try {
                CronExpression cronExp = CronExpression.parse(cron);
                LocalDateTime nextExecution = cronExp.next(now.minusSeconds(1));
                if (nextExecution != null && nextExecution.isBefore(now.plusSeconds(1))) {
                    log.info("Rule Schedule Triggered: {} cho thiết bị {}", rule.getRuleName(),
                            rule.getActionDevice().getId());

                    boolean success = irrigationContext.executeControl(
                            scheduledStrategy,
                            rule.getFarm().getId(),
                            rule.getActionDevice().getId(),
                            rule.getActionCommand().name());

                    if (success) {
                        automationRuntimeStateService.markAutoCommand(
                                rule.getActionDevice().getId(),
                                rule.getActionCommand().name(),
                                clock.instant());

                        String msg = String.format("Hệ thống tự động (Hẹn giờ): Đã %s [%s] theo luật [%s]",
                                rule.getActionCommand().name(),
                                rule.getActionDevice().getName(),
                                rule.getRuleName());
                        notificationService.createSystemNotification(rule.getFarm().getOwner().getId(), msg);
                    }
                }
            } catch (Exception e) {
                log.error("Lỗi khi xử lý scheduled rule {}: {}", rule.getId(), e.getMessage(), e);
            }
        }
    }
}
