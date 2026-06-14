package com.ecommerce.userservice.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String username;
    private String password;
    private String email;
}