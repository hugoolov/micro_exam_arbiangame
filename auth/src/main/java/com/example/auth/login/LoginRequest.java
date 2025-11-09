package com.example.auth.login;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
class LoginRequest {
    private String username;
    private String password;
}
