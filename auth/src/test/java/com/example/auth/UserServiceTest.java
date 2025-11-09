package com.example.auth;

import com.example.auth.login.AuthService;
import com.example.auth.login.User;
import com.example.auth.login.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService; // Assuming your method is in UserService

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_ShouldSaveNewUser_WhenUsernameNotExists() {
        // Arrange
        String username = "newUser";
        String password = "plainPassword";
        String encodedPassword = "encodedPassword";

        when(userRepo.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

        User savedUser = new User();
        savedUser.setUsername(username);
        savedUser.setPassword(encodedPassword);

        when(userRepo.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = authService.register(username, password);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(encodedPassword, result.getPassword());
        verify(userRepo).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenUsernameAlreadyExists() {
        // Arrange
        String username = "existingUser";
        String password = "password";

        when(userRepo.existsByUsername(username)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(username, password);
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepo, never()).save(any(User.class));
    }
}
