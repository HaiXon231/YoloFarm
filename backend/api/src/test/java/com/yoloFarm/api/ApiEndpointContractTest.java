package com.yoloFarm.api;

import com.yoloFarm.api.controller.AdminController;
import com.yoloFarm.api.controller.DeviceController;
import com.yoloFarm.api.controller.RuleController;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.service.ControlService;
import com.yoloFarm.api.service.AdminService;
import com.yoloFarm.api.service.DeviceModelService;
import com.yoloFarm.api.service.DeviceService;
import com.yoloFarm.api.service.RuleService;
import com.yoloFarm.api.service.TelemetryService;
import com.yoloFarm.api.repository.UserRepository;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.security.RestAccessDeniedHandler;
import com.yoloFarm.api.security.RestAuthenticationEntryPoint;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import com.yoloFarm.api.service.strategy.IrrigationContext;
import com.yoloFarm.api.service.strategy.ManualStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.yoloFarm.api.service.security.JwtService;

@WebMvcTest(controllers = { DeviceController.class, RuleController.class, AdminController.class })
@AutoConfigureMockMvc
class ApiEndpointContractTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private DeviceService deviceService;

        @MockitoBean
        private TelemetryService telemetryService;

        @MockitoBean
        private ControlService controlService;

        @MockitoBean
        private UserRepository userRepository;

        @MockitoBean
        private FarmRepository farmRepository;

        @MockitoBean
        private DeviceRepository deviceRepository;

        @MockitoBean
        private IMqttClient mqttClient;

        @MockitoBean
        private IrrigationContext irrigationContext;

        @MockitoBean
        private ManualStrategy manualStrategy;

        @MockitoBean
        private RuleService ruleService;

        @MockitoBean
        private DeviceModelService deviceModelService;

        @MockitoBean
        private AdminService adminService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private UserDetailsService userDetailsService;

        @MockitoBean
        private AuthenticationProvider authenticationProvider;

        @MockitoBean
        private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

        @MockitoBean
        private RestAccessDeniedHandler restAccessDeniedHandler;

        private Authentication farmerAuth;
        private Authentication adminAuth;

        @BeforeEach
        void setUp() {
                User farmer = User.builder()
                                .id(UUID.randomUUID())
                                .username("farmer")
                                .password("pwd")
                                .email("farmer@yolo.test")
                                .role(RoleEnum.FARMER)
                                .build();
                farmerAuth = new UsernamePasswordAuthenticationToken(farmer, null, farmer.getAuthorities());

                User admin = User.builder()
                                .id(UUID.randomUUID())
                                .username("admin")
                                .password("pwd")
                                .email("admin@yolo.test")
                                .role(RoleEnum.ADMIN)
                                .build();
                adminAuth = new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
        }

        @Test
        void removeDeviceRequestShouldReturnMessageObject() throws Exception {
                mockMvc.perform(post("/api/v1/devices/{deviceId}/remove-requests", UUID.randomUUID())
                                .with(authentication(farmerAuth))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("Đã gửi yêu cầu gỡ bỏ thiết bị. Vui lòng chờ Admin xác nhận thu hồi."));
        }

        @Test
        void changeModeShouldReturnMessageObject() throws Exception {
                mockMvc.perform(patch("/api/v1/devices/{deviceId}/mode", UUID.randomUUID())
                                .with(authentication(farmerAuth))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"operating_mode\":\"MANUAL\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Đã chuyển thiết bị sang chế độ MANUAL."));
        }

        @Test
        void toggleRuleShouldReturnMessageObject() throws Exception {
                mockMvc.perform(patch("/api/v1/rules/{ruleId}/toggle", UUID.randomUUID())
                                .with(authentication(farmerAuth))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"is_active\":false}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Đã tắt Rule thành công."));
        }

        @Test
        void rejectDeviceShouldReturnMessageObject() throws Exception {
                when(adminService.rejectDevice(any(), anyString()))
                                .thenReturn(Map.of("message", "Đã từ chối yêu cầu và gửi thông báo cho Nông dân."));

                mockMvc.perform(post("/api/v1/admin/devices/{deviceId}/reject", UUID.randomUUID())
                                .with(authentication(adminAuth))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reject_reason\":\"Invalid\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("Đã từ chối yêu cầu và gửi thông báo cho Nông dân."));
        }

        @Test
        void sendCommandShouldReturnOpenApiMessageShape() throws Exception {
                when(irrigationContext.executeControl(any(), any(), any(), anyString())).thenReturn(true);

                mockMvc.perform(post("/api/v1/devices/{deviceId}/command", UUID.randomUUID())
                                .with(authentication(farmerAuth))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"command\":\"ON\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("Lệnh [ON] đã được gửi tới thiết bị thành công."));
        }

        @Test
        void changeModeWithInvalidValueShouldReturn400() throws Exception {
                mockMvc.perform(patch("/api/v1/devices/{deviceId}/mode", UUID.randomUUID())
                                .with(authentication(farmerAuth))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"operating_mode\":\"XYZ\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        void sendCommandWithoutCommandShouldReturn400() throws Exception {
                mockMvc.perform(post("/api/v1/devices/{deviceId}/command", UUID.randomUUID())
                                .with(authentication(farmerAuth))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        void getDeviceRequestsWithInvalidStatusShouldReturn400() throws Exception {
                mockMvc.perform(get("/api/v1/admin/devices/requests")
                                .with(authentication(adminAuth))
                                .queryParam("status", "WRONG_STATUS"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        void approveDeviceWithInvalidFeedKeyShouldReturn400() throws Exception {
                UUID deviceId = UUID.randomUUID();

                mockMvc.perform(post("/api/v1/admin/devices/{deviceId}/approve", deviceId)
                                .with(authentication(adminAuth))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"adafruit_feed_key\":\"Cảm biến A\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400));

                verify(adminService, never()).approveDevice(any(), anyString());
        }
}
