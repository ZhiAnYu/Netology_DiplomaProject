package ru.netology.cloudService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.exception.AuthException;
import ru.netology.cloudService.repository.UserRepository;
import ru.netology.cloudService.service.AuthService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(1L, "user1", "pass123");
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        // Given
        when(userRepository.findByLogin("user1"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        String token = authService.login("user1", "pass123");

        // Then
        assertNotNull(token, "Токен не должен быть null");
        assertFalse(token.isEmpty(), "Токен не должен быть пустым");
        verify(userRepository).findByLogin("user1");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_shouldThrowException_whenUserNotFound() {
        // Given
        when(userRepository.findByLogin("unknown"))
                .thenReturn(Optional.empty());

        // When & Then
        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.login("unknown", "pass123");
        });

        assertEquals("Bad credentials", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldThrowException_whenPasswordIsWrong() {
        // Given
        when(userRepository.findByLogin("user1"))
                .thenReturn(Optional.of(testUser));

        // When & Then
        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.login("user1", "wrongpass");
        });

        assertEquals("Bad credentials", exception.getMessage());
        verify(userRepository, never()).save(any());
    }
}