package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.UserDTO;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDTO userDTO) {
        User savedUser = userService.register(userDTO);
        return ResponseEntity.ok("User registered successfully with ID: " + savedUser.getId());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDTO loginDTO) {
        String token = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
        return ResponseEntity.ok(token);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getProfile(@RequestHeader("X-User-Id") String userId) {
        User user = userService.getProfile(Long.parseLong(userId));
        return ResponseEntity.ok(user);
    }
}