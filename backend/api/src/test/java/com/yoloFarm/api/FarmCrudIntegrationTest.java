package com.yoloFarm.api;

import com.yoloFarm.api.dto.request.FarmCreateRequest;
import com.yoloFarm.api.dto.response.FarmResponse;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.repository.FarmRepository;
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
@Transactional // Quan trọng nhất: Tự động xóa sạch những cặn bẩn/dữ liệu test khỏi DB thật sau khi chạy xong
public class FarmCrudIntegrationTest {

    @MockitoBean
    private IMqttClient mqttClient;

    @Autowired
    private FarmService farmService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Test
    public void testCreateFarmAndGetByUserId() {
        // [1] - CHUẨN BỊ ĐỐI TƯỢNG THẬT (REAL OBJECT IN DB)
        User testOwner = User.builder()
                .username("real_test_user")
                .email("real_test_user@test.com")
                .password("hashed_password_placeholder")
                .role(RoleEnum.FARMER)
                .build();
        
        // Lưu thẳng vào Database (PostgreSQL sẽ ghi dữ liệu này, nhưng sẽ bị Rollback ở cuối Hàm)
        testOwner = userRepository.save(testOwner);

        // [2] - THỰC THI NGHIỆP VỤ
        FarmCreateRequest request = new FarmCreateRequest();
        request.setName("Nông Trại Trái Nhàu Tỷ Bá");
        request.setLocation("Lâm Đồng");

        FarmResponse createdResponse = farmService.createFarm(request, testOwner.getId());

        // [3] - NGHIỆM THU KẾT QUẢ KÌ VỌNG
        // Xác nhận DB đã sinh Auto-UUID thành công (không chém gió)
        assertNotNull(createdResponse.getId(), "Farm ID không được phép null sau khi save vào DB");
        
        // Xác nhận tên Nông trại truyền qua Request đã lọt thỏm an toàn vào DB và lồi lại ra Response
        assertEquals("Nông Trại Trái Nhàu Tỷ Bá", createdResponse.getName());
        assertEquals(testOwner.getId(), createdResponse.getOwnerId(), "Owner ID phải được gán cứng và khớp với User");

        // [4] - NGHIỆM THU NGHIỆP VỤ LẤY DANH SÁCH (READ)
        List<FarmResponse> userFarms = farmService.getFarmsByUserId(testOwner.getId());
        
        // Xác nhận chỉ có đúng 1 Nông trại gắn mác của User này
        assertEquals(1, userFarms.size());
        assertEquals(createdResponse.getId(), userFarms.get(0).getId());
    }
}
