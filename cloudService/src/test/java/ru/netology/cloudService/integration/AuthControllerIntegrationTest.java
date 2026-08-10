package ru.netology.cloudService.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("Интеграционные тесты AuthController")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(new User("user1", "pass123"));
    }

    @Test
    @DisplayName("POST /cloud/login: успешная авторизация возвращает токен")
    void login_shouldReturnToken_whenCredentialsAreValid() throws Exception {
        mockMvc.perform(post("/cloud/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"user1\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth-token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /cloud/login: неверный пароль возвращает 400")
    void login_shouldReturn400_whenPasswordIsWrong() throws Exception {
        mockMvc.perform(post("/cloud/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"user1\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad credentials"))
                .andExpect(jsonPath("$.id").value(0));
    }

    @Test
    @DisplayName("POST /cloud/logout: успешный выход")
    void logout_shouldReturn200_whenTokenIsValid() throws Exception {
        String token = userRepository.findByLogin("user1").orElseThrow().getAuthToken();
        if (token == null) {
            mockMvc.perform(post("/cloud/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"login\":\"user1\",\"password\":\"pass123\"}"))
                    .andExpect(status().isOk());
            token = userRepository.findByLogin("user1").orElseThrow().getAuthToken();
        }

        mockMvc.perform(post("/cloud/logout")
                        .header("auth-token", token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /cloud/list без токена возвращает 401")
    void list_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/cloud/list"))
                .andExpect(status().isUnauthorized());
    }
}