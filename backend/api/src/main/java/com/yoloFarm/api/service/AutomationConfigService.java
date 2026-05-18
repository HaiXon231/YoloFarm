package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.request.AutomationConfigRequest;
import com.yoloFarm.api.dto.response.AutomationConfigResponse;
import com.yoloFarm.api.entity.AppSetting;
import com.yoloFarm.api.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutomationConfigService {

    private static final String MAX_AUTO_ON_MINUTES = "automation.max_auto_on_minutes";
    private static final String COMMAND_COOLDOWN_SECONDS = "automation.command_cooldown_seconds";

    private final AppSettingRepository appSettingRepository;

    @Value("${app.automation.max-auto-on-minutes:20}")
    private int defaultMaxAutoOnMinutes;

    @Value("${app.automation.rule-command-cooldown-seconds:30}")
    private int defaultCommandCooldownSeconds;

    @Transactional(readOnly = true)
    public AutomationConfigResponse getConfig() {
        return AutomationConfigResponse.builder()
                .maxAutoOnMinutes(getIntSetting(MAX_AUTO_ON_MINUTES, defaultMaxAutoOnMinutes))
                .commandCooldownSeconds(getIntSetting(COMMAND_COOLDOWN_SECONDS, defaultCommandCooldownSeconds))
                .build();
    }

    @Transactional
    public AutomationConfigResponse updateConfig(AutomationConfigRequest request) {
        Integer maxAutoOnMinutes = request.getMaxAutoOnMinutes();
        Integer commandCooldownSeconds = request.getCommandCooldownSeconds();

        if (maxAutoOnMinutes == null || maxAutoOnMinutes < 1 || maxAutoOnMinutes > 1440) {
            throw new IllegalArgumentException("Giới hạn thời gian thực thi phải từ 1 đến 1440 phút");
        }
        if (commandCooldownSeconds == null || commandCooldownSeconds < 0 || commandCooldownSeconds > 86400) {
            throw new IllegalArgumentException("Cooldown lệnh phải từ 0 đến 86400 giây");
        }

        saveSetting(MAX_AUTO_ON_MINUTES, maxAutoOnMinutes);
        saveSetting(COMMAND_COOLDOWN_SECONDS, commandCooldownSeconds);
        return AutomationConfigResponse.builder()
                .maxAutoOnMinutes(maxAutoOnMinutes)
                .commandCooldownSeconds(commandCooldownSeconds)
                .build();
    }

    public int getMaxAutoOnMinutes() {
        return getConfig().getMaxAutoOnMinutes();
    }

    public int getCommandCooldownSeconds() {
        return getConfig().getCommandCooldownSeconds();
    }

    private int getIntSetting(String key, int fallback) {
        return appSettingRepository.findById(key)
                .map(AppSetting::getValue)
                .map(value -> parseIntOrFallback(value, fallback))
                .orElse(fallback);
    }

    private int parseIntOrFallback(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private void saveSetting(String key, Integer value) {
        AppSetting setting = appSettingRepository.findById(key)
                .orElseGet(() -> AppSetting.builder().key(key).build());
        setting.setValue(value.toString());
        appSettingRepository.save(setting);
    }
}
