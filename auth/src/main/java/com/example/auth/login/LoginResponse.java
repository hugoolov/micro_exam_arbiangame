package com.example.auth.login;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
class LoginResponse {
    private Long userId;
    private String username;
}