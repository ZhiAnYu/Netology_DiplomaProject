package ru.netology.cloudService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.repository.UserRepository;

// ВАЖНО: Убедитесь, что импорт LoginRequest правильный.
// Если он лежит в пакете dto, раскомментируйте строку ниже и удалите импорт из текущего пакета:
import ru.netology.cloudService.dto.LoginRequest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testDatabaseConnection() throws SQLException {
        // Проверяем, что можем получить соединение с БД
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            assertFalse(connection.isClosed());
            System.out.println("✅ Подключение к БД успешно!");
            System.out.println("URL: " + connection.getMetaData().getURL());
        }
    }

    @Test
    void testUserRepository() {
        // 1. Создаём DTO (имитируем запрос, в котором есть логин и пароль)
        LoginRequest requestDto = new LoginRequest("testuser", "testpass");

        // 2. Создаём сущность User через конструктор по умолчанию (так как другого нет)
        User user = new User();

        // 3. Переносим данные из DTO в Entity
        // Примечание: если LoginRequest у вас объявлен как 'record',
        // используйте методы requestDto.login() и requestDto.password()
        user.setLogin(requestDto.login());
        user.setPassword(requestDto.password());

        // 4. Сохраняем пользователя в базу данных
        User saved = userRepository.save(user);

        // Проверяем, что пользователь сохранён
        assertNotNull(saved.getId(), "ID должен быть сгенерирован");
        assertEquals("testuser", saved.getLogin());

        // Ищем пользователя по логину
        User found = userRepository.findByLogin("testuser").orElse(null);
        assertNotNull(found, "Пользователь должен найтись");
        assertEquals("testuser", found.getLogin());

        System.out.println("✅ UserRepository работает корректно!");

        // Удаляем тестового пользователя, чтобы не засорять базу данных после теста
        userRepository.delete(saved);
    }
}