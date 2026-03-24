package com.yoloFarm.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yoloFarm.api.dto.request.RuleCreateRequest;
import com.yoloFarm.api.service.RuleService;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleController {
    private final RuleService ruleService;

    @PostMapping
    public ResponseEntity<?> createRule(@RequestBody RuleCreateRequest request) {
        return ResponseEntity.ok(null);
    }

    @PatchMapping("/{ruleId}/toggle")
    public ResponseEntity<?> toggleRule(@PathVariable("ruleId") UUID ruleId, @RequestBody Map<String, Boolean> request) {
        return ResponseEntity.ok(null);
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<?> deleteRule(@PathVariable("ruleId") UUID ruleId) {
        return ResponseEntity.ok(null);
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<?> updateRule(@PathVariable("ruleId") UUID ruleId, @RequestBody RuleCreateRequest request) {
        return ResponseEntity.ok(null);
    }
}
