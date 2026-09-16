# 📋 README_debug.md — Отладочная информация для куратора

Этот файл содержит дополнительную информацию о текущем состоянии проекта, выполненных исправлениях и известных моментах для упрощения проверки.

---

## Выполненные исправления по замечаниям куратора

### 1. Безопасность паролей (BCrypt)
**Проблема:** Пароли хранились в открытом виде.  
**Решение:**
- Добавлена зависимость `spring-security-crypto` в `pom.xml`
- Создан `SecurityConfig.java` с `PasswordEncoder` bean
- В `AuthService` используется `passwordEncoder.matches()` вместо прямого сравнения строк
- В `data.sql` пароли хранятся в виде BCrypt-хешей

**Файлы:**
- `src/main/java/ru/netology/cloudService/config/SecurityConfig.java`
- `src/main/java/ru/netology/cloudService/service/AuthService.java`
- `src/main/resources/data.sql`

---

### 2. Обработка Bearer-префикса
**Проблема:** Фронтенд отправляет токен с префиксом `Bearer`, но бэкенд искал токен целиком.  
**Решение:** В `AuthInterceptor` добавлена универсальная обрезка префикса:
```java
if (token.contains(" ")) {
    String[] parts = token.split("\\s+");
    token = parts[parts.length - 1];
}
```
Это работает для любого префикса (`Bearer`, `Token` и т.д.) или его отсутствия.

**Файл:** `src/main/java/ru/netology/cloudService/interceptor/AuthInterceptor.java`

---

### 3. Переименование файла (filename vs name)
**Проблема:** Спецификация и фронтенд используют поле `filename`, но DTO использовало `name`.  
**Решение:**
- В `FileInfoDto` переименовано поле `name` → `filename`
- В `RenameFileRequest` переименовано поле `name` → `filename` + добавлена аннотация `@JsonProperty("filename")`
- В `FileController` обновлены вызовы с `request.name()` на `request.filename()`
- Добавлена аннотация `@Valid` в `AuthController` для корректной работы валидации

**Файлы:**
- `src/main/java/ru/netology/cloudService/dto/FileInfoDto.java`
- `src/main/java/ru/netology/cloudService/dto/RenameFileRequest.java`
- `src/main/java/ru/netology/cloudService/controller/FileController.java`
- `src/main/java/ru/netology/cloudService/controller/AuthController.java`

---

### 4. Пользователи по умолчанию
**Проблема:** После запуска приложения база данных пуста, войти невозможно.  
**Решение:**
- Создан файл `data.sql` с BCrypt-хешами для пользователей `user1` (пароль `pass123`) и `user2` (пароль `pass456`)
- В `application.yml` добавлены настройки:
  ```yaml
  spring:
    jpa:
      defer-datasource-initialization: true
    sql:
      init:
        mode: always
        encoding: UTF-8
  ```
- Теперь `data.sql` выполняется после создания таблиц Hibernate

**Файлы:**
- `src/main/resources/data.sql`
- `src/main/resources/application.yml`

---

### 5. Изоляция файлов на диске
**Проблема:** Файлы всех пользователей хранились в одной папке, что приводило к конфликтам имён.  
**Решение:**
- В `FileService` добавлен метод `getUserStoragePath(User user)`, который создаёт папку `./files/{userId}/`
- Все методы работы с файлами (`uploadFile`, `downloadFile`, `renameFile`, `deleteFile`) обновлены для использования этой папки
- Теперь файлы разных пользователей не конфликтуют, даже если имеют одинаковые имена

**Структура:**
```
./files/
├── 1/                    ← папка для user1 (id=1)
│   ├── document.txt
│   └── report.pdf
├── 2/                    ← папка для user2 (id=2)
│   └── document.txt      ← не конфликтует с user1!
```

**Файл:** `src/main/java/ru/netology/cloudService/service/FileService.java`

---

### 6. Удалён отладочный вывод
**Проблема:** В `AuthInterceptor` остался `System.out.println`.  
**Решение:** Строка удалена, оставлен только `log.debug()`.

**Файл:** `src/main/java/ru/netology/cloudService/interceptor/AuthInterceptor.java`

---

### 7. Очистка репозитория
**Проблема:** В репозиторий закоммичены логи, загруженные файлы, настройки IDE. `.gitignore` лежал внутри модуля.  
**Решение:**
- `.gitignore` перемещён в корень репозитория (`Netology_DiplomaProject/`)
- Из Git-индекса удалены папки: `.idea/`, `target/`, `logs/`, `files/`, `app-files/`
- Добавлены правила для игнорирования логов, скомпилированных классов, загруженных файлов

**Файл:** `.gitignore` (в корне репозитория)

---

## Инструкция по запуску

### Вариант 1: Через Docker (рекомендуется)

```bash
# Перейти в папку проекта
cd cloudService

# Запустить все сервисы (PostgreSQL + Backend + Nginx)
docker-compose up -d --build

# Проверить логи
docker-compose logs backend

# Ожидаемая строка в логах:
# Started CloudServiceApplication in X.XXX seconds
```

**Сервисы:**
- PostgreSQL: `localhost:5432`
- Backend: `localhost:8080`
- Nginx: `localhost:80`

---

### Вариант 2: Локально в IDE

1. Запустить PostgreSQL в Docker:
   ```bash
   docker-compose up -d postgres
   ```

2. Открыть проект в IntelliJ IDEA
3. Запустить `CloudServiceApplication.java`
4. Backend будет доступен на `http://localhost:8080/cloud`

---

2. Запустить фронтенд:
   ```bash
   npm run serve
   ```

3. Открыть в браузере адрес из терминала (обычно `http://localhost:8081`)

---

## 👥 Пользователи по умолчанию

После запуска приложения в базе данных автоматически создаются два тестовых пользователя:

| Логин | Пароль |
|-------|--------|
| `user1` | `pass123` |
| `user2` | `pass456` |

Пароли хранятся в базе данных в виде BCrypt-хешей (не в открытом виде).

---

## Диагностика проблем

### Проблема: Контейнер backend падает с ошибкой `relation "users" does not exist`
**Причина:** `data.sql` выполняется до создания таблиц Hibernate.  
**Решение:** Убедиться, что в `application.yml` есть:
```yaml
spring:
  jpa:
    defer-datasource-initialization: true
  sql:
    init:
      mode: always
```

---

### Проблема: Ошибка CORS в консоли браузера
**Причина:** Порт фронтенда не добавлен в `allowedOrigins`.  
**Решение:** В `WebConfig.java` добавить порт фронтенда:
```java
.allowedOrigins("http://localhost:8080", "http://localhost:8081")
```

---

### Проблема: Ошибка 404 при запросах
**Причина:** В `.env` фронтенда не указан `/cloud` в конце URL.  
**Решение:** Убедиться, что в `.env` написано:
```env
VUE_APP_BASE_URL=http://localhost:8080/cloud
```

---

---

## Тестирование

### Unit-тесты (Mockito)
```bash
mvn test -Dtest="*Test"
```

### Интеграционные тесты (Testcontainers)
 Требуют запущенный Docker Desktop!
```bash
mvn test -Dtest="*IntegrationTest"
```

### Пропустить все тесты при сборке
```bash
mvn clean package -DskipTests
```

---

##  Известные ограничения

1. **Токены не имеют срока действия** — реализовано как простые UUID без истечения. Для production рекомендуется использовать JWT с `exp` claim.

2. **Нет механизма восстановления удалённых файлов** — soft delete помечает файлы как `deleted=true`, но UI для восстановления не реализован (это можно добавить в будущем).

3. **Spring Security не используется полностью** — подключён только модуль `spring-security-crypto` для BCrypt. Полная интеграция Spring Security с JWT и SecurityFilterChain запланирована на следующий этап развития проекта.

4. **Миграции БД не используются** — структура таблиц создаётся через `ddl-auto: update`. Для production рекомендуется использовать Flyway или Liquibase.

---
