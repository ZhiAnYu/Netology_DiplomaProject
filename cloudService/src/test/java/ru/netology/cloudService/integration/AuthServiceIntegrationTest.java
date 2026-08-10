package ru.netology.cloudService.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.exception.AuthException;
import ru.netology.cloudService.repository.UserRepository;
import ru.netology.cloudService.service.AuthService;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Интеграционные тесты AuthService")
class AuthServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Успешный логин: токен генерируется и сохраняется в БД")
    void login_shouldGenerateAndSaveToken_whenCredentialsAreValid() {
        // Given:
        User user = new User("user1", "pass123");
        userRepository.save(user);

        // When:
        String token = authService.login("user1", "pass123");

        // Then
        assertNotNull(token);
        assertFalse(token.isBlank());

        User updatedUser = userRepository.findByLogin("user1").orElseThrow();
        assertEquals(token, updatedUser.getAuthToken());
    }

    @Test
    @DisplayName("Неверный пароль → AuthException")
    void login_shouldThrowException_whenPasswordIsWrong() {
        // Given
        userRepository.save(new User("user1", "pass123"));

        // When & Then
        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.login("user1", "wrongpass");
        });

        assertEquals("Bad credentials", exception.getMessage());
    }

    @Test
    @DisplayName("Logout: токен удаляется из БД")
    void logout_shouldRemoveTokenFromDatabase() {
        // Given:
        userRepository.save(new User("user1", "pass123"));
        String token = authService.login("user1", "pass123");
        // When:
        authService.logout(token);
        // Then:
        User user = userRepository.findByLogin("user1").orElseThrow();
        assertNull(user.getAuthToken());
    }

    @Test
    @DisplayName("getUserByToken: возвращает пользователя по валидному токену")
    void getUserByToken_shouldReturnUser_whenTokenIsValid() {
        // Given
        userRepository.save(new User("user1", "pass123"));
        String token = authService.login("user1", "pass123");
        // When
        User foundUser = authService.getUserByToken(token);
        // Then
        assertNotNull(foundUser);
        assertEquals("user1", foundUser.getLogin());
    }
}