package com.yoloFarm.api.service.impl;

import com.yoloFarm.api.dto.request.LoginRequest;
import com.yoloFarm.api.dto.request.RegisterRequest;
import com.yoloFarm.api.dto.response.LoginResponse;
import com.yoloFarm.api.dto.response.UserProfile;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.exception.ConflictException;
import com.yoloFarm.api.repository.UserRepository;
import com.yoloFarm.api.service.AuthService;
import com.yoloFarm.api.service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public UserProfile register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent() ||
            userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("Tên đăng nhập hoặc Email đã tồn tại!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(RoleEnum.FARMER);  // Mặc định là FARMER theo ERD/OpenAPI
        
        User savedUser = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setId(savedUser.getId());
        profile.setUsername(savedUser.getUsername());
        profile.setEmail(savedUser.getEmail());
        profile.setRole(savedUser.getRole().name());
        profile.setCreatedAt(savedUser.getCreatedAt());
        return profile;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Uỷ quyền cho Spring Security check credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
        
        String jwtToken = jwtService.generateToken(user);
        
        LoginResponse response = new LoginResponse();
        response.setAccessToken(jwtToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(86400); // 24 giờ
        response.setRole(user.getRole().name());
        
        return response;
    }
}
