package ru.netology.cloudService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.exception.AuthException;
import ru.netology.cloudService.repository.UserRepository;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public String login(String login, String password) {
        log.info("Попытка входа пользователя: {}", login);

        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> {
                    log.warn("Пользователь не найден: {}", login);
                    return new AuthException("Bad credentials");
                });

        if (!user.getPassword().equals(password)) {
            log.warn("Неверный пароль для пользователя: {}", login);
            throw new AuthException("Bad credentials");
        }

        String token = UUID.randomUUID().toString();

        user.setAuthToken(token);
        userRepository.save(user);

        log.info("Успешный вход пользователя: {}", login);
        return token;
    }

    @Transactional
    public void logout(String token) {
        User user = userRepository.findByAuthToken(token)
                .orElseThrow(() -> new AuthException("Invalid token"));

        user.setAuthToken(null); // удаляем токен
        userRepository.save(user);

        log.info("Пользователь {} вышел из системы", user.getLogin());
    }

    @Transactional(readOnly = true)
    public User getUserByToken(String token) {
        return userRepository.findByAuthToken(token)
                .orElseThrow(() -> new AuthException("Unauthorized"));
    }
}