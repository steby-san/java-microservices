package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.UserDTO;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.userservice.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.jwtUtil = jwtUtil;
    }

    public User register(UserDTO dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword())); // Mã hóa BCrypt
        user.setRole("ROLE_USER");
        return userRepository.save(user);
    }

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtUtil.generateToken(user.getId().toString(), user.getUsername());
    }

    public User getProfile(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}