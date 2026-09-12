package ru.netology.cloudService.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import ru.netology.cloudService.dto.FileInfoDto;
import ru.netology.cloudService.entity.FileInfo;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.exception.FileNotFoundException;
import ru.netology.cloudService.repository.FileRepository;
import ru.netology.cloudService.repository.UserRepository;
import ru.netology.cloudService.service.FileService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Интеграционные тесты FileService")
class FileServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;

    @BeforeEach
    void setUp() throws IOException {
        jdbcTemplate.execute("DELETE FROM files");
        jdbcTemplate.execute("DELETE FROM users");

        testUser = userRepository.save(new User("user1", "pass123"));

        Path tempDir = Files.createTempDirectory("cloud-test-");
        ReflectionTestUtils.setField(fileService, "fileStoragePath", tempDir);
    }

    @Test
    @DisplayName("Полный цикл: загрузка → список → скачивание → удаление")
    void fullFileLifecycle() throws IOException {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello, Cloud!".getBytes());

        fileService.uploadFile(testUser, "test.txt", mockFile);

        List<FileInfoDto> files = fileService.getUserFiles(testUser, 10);
        assertEquals(1, files.size());
        assertEquals("test.txt", files.get(0).filename());

        var resource = fileService.downloadFile(testUser, "test.txt");
        assertNotNull(resource);
        assertEquals("test.txt", resource.getFilename());

        fileService.deleteFile(testUser, "test.txt");

        List<FileInfoDto> filesAfterDelete = fileService.getUserFiles(testUser, 10);
        assertTrue(filesAfterDelete.isEmpty(), "Файл должен исчезнуть из списка");
    }

    @Test
    @DisplayName("Soft delete: запись остаётся в БД с deleted=true")
    void softDelete_shouldKeepRecordInDatabaseWithDeletedFlag() throws IOException {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "content".getBytes());
        fileService.uploadFile(testUser, "test.txt", mockFile);

        // When:
        fileService.deleteFile(testUser, "test.txt");

        // Then:
        List<FileInfo> allFilesIncludingDeleted =
                fileRepository.findAllByUserIdIncludingDeleted(testUser.getId());

        assertEquals(1, allFilesIncludingDeleted.size());
        assertTrue(allFilesIncludingDeleted.get(0).isDeleted(),
                "Флаг deleted должен быть true");
    }

    @Test
    @DisplayName("@Where работает: обычные запросы не видят удалённые файлы")
    void whereAnnotation_shouldFilterDeletedFiles() throws IOException {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "content".getBytes());
        fileService.uploadFile(testUser, "test.txt", mockFile);
        fileService.deleteFile(testUser, "test.txt");

        // When:
        var found = fileRepository.findByUserAndFilename(testUser, "test.txt");

        // Then:
        assertTrue(found.isEmpty(), "@Where должен отфильтровать удалённые файлы");
    }

    @Test
    @DisplayName("Изоляция пользователей: user2 не видит файлы user1")
    void userIsolation_shouldNotSeeOtherUserFiles() throws IOException {
        // Given
        User user2 = userRepository.save(new User("user2", "pass456"));

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "secret.txt", "text/plain", "secret content".getBytes());
        fileService.uploadFile(testUser, "secret.txt", mockFile);

        // When:
        List<FileInfoDto> user2Files = fileService.getUserFiles(user2, 10);

        // Then:
        assertTrue(user2Files.isEmpty(), "user2 не должен видеть файлы user1");

        // When:
        // Then:
        assertThrows(FileNotFoundException.class, () -> {
            fileService.downloadFile(user2, "secret.txt");
        });
    }

    @Test
    @DisplayName("Переименование файла обновляет и диск, и БД")
    void renameFile_shouldUpdateDiskAndDatabase() throws IOException {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "old.txt", "text/plain", "content".getBytes());
        fileService.uploadFile(testUser, "old.txt", mockFile);

        // When
        fileService.renameFile(testUser, "old.txt", "new.txt");

        // Then:
        var found = fileRepository.findByUserAndFilename(testUser, "new.txt");
        assertTrue(found.isPresent());
        assertEquals("new.txt", found.get().getFilename());

        var oldFound = fileRepository.findByUserAndFilename(testUser, "old.txt");
        assertTrue(oldFound.isEmpty());
    }
}