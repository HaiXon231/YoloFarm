package com.yoloFarm.api;

import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class EntityMappingIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testUserEntityMapping() {
        User user = User.builder()
                .username("test_farmer")
                .email("farmer@test.com")
                .password("hashed_pass")
                .role(RoleEnum.FARMER)
                .build();

        user = userRepository.save(user);

        assertNotNull(user.getId(), "User ID phải được tự động sinh (UUID) sau khi save vào H2 DB");
        assertEquals("test_farmer", user.getUsername());
    }
}
