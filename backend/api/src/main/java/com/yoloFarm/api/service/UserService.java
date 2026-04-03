package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.request.UpdateProfileRequest;
import com.yoloFarm.api.dto.response.UserProfile;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.exception.ConflictException;
import com.yoloFarm.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfile getCurrentProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        
        return mapToProfile(user);
    }

    @Transactional(readOnly = true)
    public java.util.List<UserProfile> getAllProfiles() {
        return userRepository.findAll().stream()
                .map(this::mapToProfile)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public UserProfile updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // Cập nhật Username nếu có và không trùng
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new ConflictException("Tên người dùng này đã tồn tại!");
            }
            user.setUsername(request.getUsername());
        }

        // Cập nhật Email nếu có và không trùng
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new ConflictException("Email này đã được sử dụng!");
            }
            user.setEmail(request.getEmail());
        }

        // Đổi mật khẩu nếu có yêu cầu
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
                throw new RuntimeException("Bạn phải nhập mật khẩu hiện tại để đổi mật khẩu mới!");
            }
            
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("Mật khẩu hiện tại không chính xác!");
            }
            
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        User savedUser = userRepository.save(user);
        return mapToProfile(savedUser);
    }

    private UserProfile mapToProfile(User user) {
        UserProfile profile = new UserProfile();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setEmail(user.getEmail());
        profile.setRole(user.getRole().name());
        profile.setCreatedAt(user.getCreatedAt());
        return profile;
    }
}
