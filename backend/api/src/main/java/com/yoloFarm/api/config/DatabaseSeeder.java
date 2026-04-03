package com.yoloFarm.api.config;

import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@yolofarm.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(RoleEnum.ADMIN)
                    .build();
            userRepository.save(admin);
            System.out.println(">>> Default Admin account created: admin / admin123");
        }
        
        if (userRepository.findByUsername("farmer1").isEmpty()) {
            User farmer = User.builder()
                    .username("farmer1")
                    .email("farmer1@yolofarm.com")
                    .password(passwordEncoder.encode("password"))
                    .role(RoleEnum.FARMER)
                    .build();
            userRepository.save(farmer);
            System.out.println(">>> Default Farmer account created: farmer1 / password");
        }
    }
}
