package com.yoloFarm.api.security;

import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.UserRepository;
import com.yoloFarm.api.service.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final FarmRepository farmRepository;

    private static final String SESSION_USER_ID = "wsUserId";
    private static final String SESSION_IS_ADMIN = "wsIsAdmin";

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnection(accessor);
            accessor.setLeaveMutable(true);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticateConnection(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Thiếu JWT token cho kết nối WebSocket");
        }

        String jwt = authHeader.substring(7);
        String username = jwtService.extractUsername(jwt);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(jwt, userDetails)) {
            throw new AccessDeniedException("JWT không hợp lệ cho kết nối WebSocket");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());
        accessor.setUser(authentication);

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            userRepository.findByUsername(username)
                    .map(User::getId)
                    .ifPresent(userId -> sessionAttributes.put(SESSION_USER_ID, userId.toString()));
            sessionAttributes.put(SESSION_IS_ADMIN, isAdmin(authentication));
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/topic/farm/") || !destination.endsWith("/telemetry")) {
            return;
        }

        Principal principal = accessor.getUser();

        UUID farmId;
        try {
            String farmIdPart = destination.substring("/topic/farm/".length(),
                    destination.length() - "/telemetry".length());
            if (farmIdPart.endsWith("/")) {
                farmIdPart = farmIdPart.substring(0, farmIdPart.length() - 1);
            }
            farmId = UUID.fromString(farmIdPart);
        } catch (Exception ex) {
            throw new AccessDeniedException("Định dạng kênh telemetry không hợp lệ");
        }

        UUID userId = resolveUserId(principal, accessor.getSessionAttributes());
        if (userId == null) {
            throw new AccessDeniedException("Không thể xác định chủ thể WebSocket");
        }

        if (isAdmin(principal, accessor.getSessionAttributes())) {
            return;
        }

        if (!farmRepository.existsByIdAndOwnerId(farmId, userId)) {
            log.warn("WebSocket subscribe blocked: userId={} farmId={} destination={}", userId, farmId, destination);
            throw new AccessDeniedException("Bạn không có quyền subscribe telemetry của farm này");
        }
    }

    private UUID resolveUserId(Principal principal, Map<String, Object> sessionAttributes) {
        if (principal instanceof Authentication authentication) {
            Object p = authentication.getPrincipal();
            if (p instanceof User user) {
                return user.getId();
            }
            UUID resolved = userRepository.findByUsername(authentication.getName()).map(User::getId).orElse(null);
            if (resolved != null) {
                return resolved;
            }
        }

        if (principal != null) {
            UUID resolved = userRepository.findByUsername(principal.getName()).map(User::getId).orElse(null);
            if (resolved != null) {
                return resolved;
            }
        }

        if (sessionAttributes != null) {
            Object userId = sessionAttributes.get(SESSION_USER_ID);
            if (userId instanceof String userIdText) {
                try {
                    return UUID.fromString(userIdText);
                } catch (IllegalArgumentException ignored) {
                    log.warn("Invalid SESSION_USER_ID: {}", userIdText);
                }
            }
        }

        return null;
    }

    private boolean isAdmin(Principal principal, Map<String, Object> sessionAttributes) {
        if (principal instanceof Authentication authentication) {
            return isAdmin(authentication);
        }

        if (sessionAttributes != null) {
            Object adminFlag = sessionAttributes.get(SESSION_IS_ADMIN);
            if (adminFlag instanceof Boolean isAdmin) {
                return isAdmin;
            }
        }

        return false;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
