package ru.netology.cloudService.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import ru.netology.cloudService.dto.FileInfoDto;
import ru.netology.cloudService.entity.FileInfo;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.exception.FileNotFoundException;
import ru.netology.cloudService.repository.FileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты FileService")
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    private FileService fileService;

    @TempDir
    Path tempDir;
    private User testUser;

    @BeforeEach
    void setUp() {
        fileService = new FileService(fileRepository, tempDir.toString());
        testUser = new User(1L, "user1", "pass123");
    }

    @Nested
    @DisplayName("Метод uploadFile")
    class UploadFileTests {

        @Test
        @DisplayName("Успешная загрузка файла")
        void uploadFile_shouldSaveFileToDiskAndDatabase() throws IOException {
            // Given
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "Hello, World!".getBytes());

            when(fileRepository.existsByUserAndFilename(testUser, "test.txt")).thenReturn(false);
            when(fileRepository.save(any(FileInfo.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            fileService.uploadFile(testUser, "test.txt", mockFile);

            // Then
            Path savedFile = tempDir.resolve("1").resolve("test.txt");
            assertTrue(Files.exists(savedFile), "Файл должен быть создан на диске");
            assertEquals("Hello, World!", Files.readString(savedFile));

            ArgumentCaptor<FileInfo> captor = ArgumentCaptor.forClass(FileInfo.class);
            verify(fileRepository).save(captor.capture());

            FileInfo saved = captor.getValue();
            assertEquals("test.txt", saved.getFilename());
            assertEquals(mockFile.getSize(), saved.getSize());
            assertEquals(testUser, saved.getUser());
            assertFalse(saved.isDeleted());
        }

        @Test
        @DisplayName("Попытка загрузить пустой файл → IllegalArgumentException")
        void uploadFile_shouldThrowException_whenFileIsEmpty() {
            // Given
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "empty.txt", "text/plain", new byte[0]);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                fileService.uploadFile(testUser, "empty.txt", emptyFile);
            });

            assertEquals("Файл пуст", exception.getMessage());
            verify(fileRepository, never()).save(any());
        }

        @Test
        @DisplayName("Попытка загрузить файл с существующим именем → IllegalArgumentException")
        void uploadFile_shouldThrowException_whenFileAlreadyExists() {
            // Given
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "content".getBytes());
            when(fileRepository.existsByUserAndFilename(testUser, "test.txt")).thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                fileService.uploadFile(testUser, "test.txt", mockFile);
            });

            assertEquals("Файл с таким именем уже существует", exception.getMessage());
            verify(fileRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Метод getUserFiles")
    class GetUserFilesTests {

        @Test
        @DisplayName("Возвращает список файлов пользователя")
        void getUserFiles_shouldReturnListOfFiles() {
            // Given
            FileInfo file1 = FileInfo.builder().filename("file1.txt").size(100L).user(testUser).deleted(false).build();
            FileInfo file2 = FileInfo.builder().filename("file2.txt").size(200L).user(testUser).deleted(false).build();

            when(fileRepository.findByUser(eq(testUser), any()))
                    .thenReturn(List.of(file1, file2));

            // When
            List<FileInfoDto> result = fileService.getUserFiles(testUser, 10);

            // Then
            assertEquals("file1.txt", result.get(0).filename());
            assertEquals(100L, result.get(0).size());

            assertEquals("file2.txt", result.get(1).filename());
            assertEquals(200L, result.get(1).size());
        }

        @Test
        @DisplayName("Возвращает пустой список, если файлов нет")
        void getUserFiles_shouldReturnEmptyList_whenNoFiles() {
            // Given
            when(fileRepository.findByUser(eq(testUser), any()))
                    .thenReturn(Collections.emptyList());

            // When
            List<FileInfoDto> result = fileService.getUserFiles(testUser, 10);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Метод downloadFile")
    class DownloadFileTests {

        @Test
        @DisplayName("Успешное скачивание файла")
        void downloadFile_shouldReturnResource_whenFileExists() throws IOException {
            // Given
            // Создаём реальный файл в папке пользователя (id=1) внутри tempDir
            Path userFolder = tempDir.resolve("1");
            Files.createDirectories(userFolder);
            Path testFile = userFolder.resolve("test.txt");
            Files.writeString(testFile, "Hello, World!");

            FileInfo fileInfo = FileInfo.builder()
                    .filename("test.txt")
                    .size(13L)
                    .user(testUser)
                    .deleted(false)
                    .build();

            when(fileRepository.findByUserAndFilename(testUser, "test.txt"))
                    .thenReturn(Optional.of(fileInfo));

            // When
            var resource = fileService.downloadFile(testUser, "test.txt");

            // Then
            assertNotNull(resource);
            assertTrue(resource.isReadable());
            assertEquals("test.txt", resource.getFilename());
        }

        @Test
        @DisplayName("Файл не найден в БД → FileNotFoundException")
        void downloadFile_shouldThrowException_whenFileNotInDatabase() {
            // Given
            when(fileRepository.findByUserAndFilename(testUser, "nonexistent.txt"))
                    .thenReturn(Optional.empty());

            // When & Then
            FileNotFoundException exception = assertThrows(FileNotFoundException.class, () -> {
                fileService.downloadFile(testUser, "nonexistent.txt");
            });

            assertEquals("Файл не найден", exception.getMessage());
        }

        @Test
        @DisplayName("Файл есть в БД, но отсутствует на диске → RuntimeException")
        void downloadFile_shouldThrowException_whenFileNotOnDisk() {
            // Given
            FileInfo fileInfo = FileInfo.builder()
                    .filename("missing.txt")
                    .size(100L)
                    .user(testUser)
                    .deleted(false)
                    .build();

            when(fileRepository.findByUserAndFilename(testUser, "missing.txt"))
                    .thenReturn(Optional.of(fileInfo));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                fileService.downloadFile(testUser, "missing.txt");
            });

            assertTrue(exception.getMessage().contains("отсутствует на диске")
                    || exception.getMessage().contains("повреждён"));
        }
    }

    @Nested
    @DisplayName("Метод deleteFile")
    class DeleteFileTests {

        @Test
        @DisplayName("Успешное мягкое удаление файла")
        void deleteFile_shouldCallRepositoryDelete() {
            // Given
            FileInfo fileInfo = FileInfo.builder()
                    .id(1L)
                    .filename("test.txt")
                    .size(100L)
                    .user(testUser)
                    .deleted(false)
                    .build();

            when(fileRepository.findByUserAndFilename(testUser, "test.txt"))
                    .thenReturn(Optional.of(fileInfo));

            // When
            fileService.deleteFile(testUser, "test.txt");

            // Then
            verify(fileRepository).delete(fileInfo);
        }

        @Test
        @DisplayName("Попытка удалить несуществующий файл → FileNotFoundException")
        void deleteFile_shouldThrowException_whenFileNotFound() {
            // Given
            when(fileRepository.findByUserAndFilename(testUser, "nonexistent.txt"))
                    .thenReturn(Optional.empty());

            // When & Then
            FileNotFoundException exception = assertThrows(FileNotFoundException.class, () -> {
                fileService.deleteFile(testUser, "nonexistent.txt");
            });

            assertEquals("Файл не найден", exception.getMessage());
            verify(fileRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Метод renameFile")
    class RenameFileTests {

        @Test
        @DisplayName("Успешное переименование файла")
        void renameFile_shouldRenameOnDiskAndDatabase() throws IOException {
            // Given
            Path userFolder = tempDir.resolve("1");
            Files.createDirectories(userFolder);
            Path oldFile = userFolder.resolve("old.txt");
            Files.writeString(oldFile, "content");

            FileInfo fileInfo = FileInfo.builder()
                    .id(1L)
                    .filename("old.txt")
                    .size(7L)
                    .user(testUser)
                    .deleted(false)
                    .build();

            when(fileRepository.findByUserAndFilename(testUser, "old.txt"))
                    .thenReturn(Optional.of(fileInfo));
            when(fileRepository.existsByUserAndFilename(testUser, "new.txt"))
                    .thenReturn(false);
            when(fileRepository.save(any(FileInfo.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            fileService.renameFile(testUser, "old.txt", "new.txt");

            // Then
            // 1. Файл на диске переименован
            assertFalse(Files.exists(oldFile), "Старый файл должен исчезнуть");
            Path newFile = userFolder.resolve("new.txt");
            assertTrue(Files.exists(newFile), "Новый файл должен появиться в папке пользователя");
            assertEquals("content", Files.readString(newFile));

            // 2. В БД обновлено имя
            verify(fileRepository).save(fileInfo);
            assertEquals("new.txt", fileInfo.getFilename());
        }

        @Test
        @DisplayName("Старый файл не найден → FileNotFoundException")
        void renameFile_shouldThrowException_whenOldFileNotFound() {
            // Given
            when(fileRepository.findByUserAndFilename(testUser, "nonexistent.txt"))
                    .thenReturn(Optional.empty());

            // When & Then
            FileNotFoundException exception = assertThrows(FileNotFoundException.class, () -> {
                fileService.renameFile(testUser, "nonexistent.txt", "new.txt");
            });

            assertEquals("Файл не найден", exception.getMessage());
        }

        @Test
        @DisplayName("Новое имя уже занято → IllegalArgumentException")
        void renameFile_shouldThrowException_whenNewNameAlreadyExists() throws IOException {
            // Given
            Path oldFile = tempDir.resolve("old.txt");
            Files.writeString(oldFile, "content");

            FileInfo fileInfo = FileInfo.builder()
                    .filename("old.txt")
                    .size(7L)
                    .user(testUser)
                    .deleted(false)
                    .build();

            when(fileRepository.findByUserAndFilename(testUser, "old.txt"))
                    .thenReturn(Optional.of(fileInfo));
            when(fileRepository.existsByUserAndFilename(testUser, "existing.txt"))
                    .thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                fileService.renameFile(testUser, "old.txt", "existing.txt");
            });

            assertEquals("Файл с таким новым именем уже существует", exception.getMessage());
            verify(fileRepository, never()).save(any());
        }
    }
}