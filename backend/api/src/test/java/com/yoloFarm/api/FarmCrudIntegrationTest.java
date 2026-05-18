package com.yoloFarm.api;

import com.yoloFarm.api.dto.request.FarmCreateRequest;
import com.yoloFarm.api.dto.response.FarmResponse;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.repository.UserRepository;
import com.yoloFarm.api.service.FarmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.eclipse.paho.client.mqttv3.IMqttClient;

@SpringBootTest
@Transactional
public class FarmCrudIntegrationTest {

    @MockitoBean
    private IMqttClient mqttClient;

    @Autowired
    private FarmService farmService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testCreateFarmAndGetByUserId() {
        User testOwner = User.builder()
                .username("real_test_user")
                .email("real_test_user@test.com")
                .password("hashed_password_placeholder")
                .role(RoleEnum.FARMER)
                .build();

        testOwner = userRepository.save(testOwner);

        FarmCreateRequest request = new FarmCreateRequest();
        request.setName("Nông Trại Trái Nhàu Tỷ Bá");
        request.setLocation("Lâm Đồng");

        FarmResponse createdResponse = farmService.createFarm(request, testOwner.getId());

        assertNotNull(createdResponse.getId(), "Farm ID không được phép null sau khi save vào DB");

        assertEquals("Nông Trại Trái Nhàu Tỷ Bá", createdResponse.getName());
        assertEquals(testOwner.getId(), createdResponse.getOwnerId(), "Owner ID phải được gán cứng và khớp với User");

        List<FarmResponse> userFarms = farmService.getFarmsByUserId(testOwner.getId());

        assertEquals(1, userFarms.size());
        assertEquals(createdResponse.getId(), userFarms.get(0).getId());
    }
}
