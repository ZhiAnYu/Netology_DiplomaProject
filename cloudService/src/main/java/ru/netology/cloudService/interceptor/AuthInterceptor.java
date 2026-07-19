package ru.netology.cloudService.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.exception.UnauthorizedException;
import ru.netology.cloudService.service.AuthService;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    private static final String AUTH_TOKEN_HEADER = "auth-token";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        System.out.println(">>> INTERCEPTOR ВЫЗВАН: " + request.getRequestURI());

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = request.getHeader(AUTH_TOKEN_HEADER);

        log.debug("Запрос: {} {} | Токен: {}",
                request.getMethod(),
                request.getRequestURI(),
                token != null ? "присутствует" : "отсутствует");

        if (token == null || token.isBlank()) {
            log.warn("Отсутствует заголовок {}", AUTH_TOKEN_HEADER);
            throw new UnauthorizedException("Missing auth token");
        }

        try {
            User user = authService.getUserByToken(token);
            log.debug("Пользователь {} авторизован", user.getLogin());

            request.setAttribute("currentUser", user);

            return true;
        } catch (Exception e) {
            log.warn("Невалидный токен: {}", token);
            throw new UnauthorizedException("Invalid auth token");
        }
    }
}