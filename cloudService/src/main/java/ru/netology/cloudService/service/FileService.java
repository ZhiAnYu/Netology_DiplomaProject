package ru.netology.cloudService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudService.dto.FileInfoDto;
import ru.netology.cloudService.entity.FileInfo;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.exception.FileNotFoundException;
import ru.netology.cloudService.repository.FileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;
    private final Path fileStoragePath;

    public FileService(FileRepository fileRepository,
                       @Value("${file-storage.path:./files}") String storagePath) {
        this.fileRepository = fileRepository;
        this.fileStoragePath = Paths.get(storagePath).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStoragePath);
            log.info("Директория для файлов: {}", this.fileStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать директорию для файлов", e);
        }
    }

    @Transactional
    public void uploadFile(User user, String filename, MultipartFile file) {
        log.info("Попытка загрузки файла: {} для пользователя: {}", filename, user.getLogin());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пуст");
        }

        if (fileRepository.existsByUserAndFilename(user, filename)) {
            throw new IllegalArgumentException("Файл с таким именем уже существует");
        }

        try {
            Path targetLocation = this.fileStoragePath.resolve(filename).normalize();

            if (!targetLocation.startsWith(this.fileStoragePath)) {
                throw new IllegalArgumentException("Небезопасный путь к файлу");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Файл успешно сохранён на диск: {}", targetLocation);
        } catch (IOException e) {
            log.error("Ошибка при сохранении файла на диск", e);
            throw new RuntimeException("Ошибка при сохранении файла", e);
        }

        FileInfo fileInfo = FileInfo.builder()
                .filename(filename)
                .size(file.getSize())
                .user(user)
                .deleted(false)
                .build();

        fileRepository.save(fileInfo);
        log.info("Метаданные файла сохранены в БД");
    }

    @Transactional(readOnly = true)
    public org.springframework.core.io.Resource downloadFile(User user, String filename) {
        log.info("Попытка скачивания файла: {} для пользователя: {}", filename, user.getLogin());

        // 1. Ищем файл в БД (автоматически фильтрует deleted = false благодаря @Where)
        FileInfo fileInfo = fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> {
                    log.warn("Файл не найден: {}", filename);
                    return new FileNotFoundException("Файл не найден");
                });

        // 2. Проверяем, что файл существует на диске
        Path filePath = this.fileStoragePath.resolve(fileInfo.getFilename()).normalize();

        if (!Files.exists(filePath)) {
            log.error("Файл есть в БД, но отсутствует на диске: {}", filePath);
            throw new FileNotFoundException("Файл повреждён или отсутствует на диске");
        }

        // 3. Возвращаем ресурс для скачивания
        try {
            org.springframework.core.io.Resource resource =
                    new org.springframework.core.io.UrlResource(filePath.toUri());

            if (resource.isReadable()) {
                log.info("Файл готов к скачиванию: {}", filename);
                return resource;
            } else {
                throw new RuntimeException("Файл недоступен для чтения");
            }
        } catch (Exception e) {
            log.error("Ошибка при подготовке файла к скачиванию", e);
            throw new RuntimeException("Ошибка при скачивании файла", e);
        }
    }

    @Transactional
    public void renameFile(User user, String oldFilename, String newFilename) {
        log.info("Попытка переименования файла: {} -> {} для пользователя: {}", oldFilename, newFilename, user.getLogin());

        FileInfo fileInfo = fileRepository.findByUserAndFilename(user, oldFilename)
                .orElseThrow(() -> {
                    log.warn("Файл для переименования не найден: {}", oldFilename);
                    return new FileNotFoundException("Файл не найден");
                });

        if (fileRepository.existsByUserAndFilename(user, newFilename)) {
            throw new IllegalArgumentException("Файл с таким новым именем уже существует");
        }

        Path oldPath = this.fileStoragePath.resolve(oldFilename).normalize();
        Path newPath = this.fileStoragePath.resolve(newFilename).normalize();

        if (!oldPath.startsWith(this.fileStoragePath) || !newPath.startsWith(this.fileStoragePath)) {
            throw new IllegalArgumentException("Небезопасный путь к файлу");
        }
        try {
            Files.move(oldPath, newPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Файл успешно переименован на диске");
        } catch (java.io.IOException e) {
            log.error("Ошибка при переименовании файла на диске", e);
            throw new RuntimeException("Ошибка файловой системы при переименовании");
        }

        fileInfo.setFilename(newFilename);
        fileRepository.save(fileInfo);
        log.info("Имя файла обновлено в БД");
    }

    @Transactional
    public void deleteFile(User user, String filename) {
        log.info("Попытка удаления файла: {} для пользователя: {}", filename, user.getLogin());
        FileInfo fileInfo = fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> {
                    log.warn("Файл для удаления не найден: {}", filename);
                    return new FileNotFoundException("Файл не найден");
                });
        fileRepository.delete(fileInfo);
        log.info("Файл {} помечен как удаленный (soft delete)", filename);
    }

    @Transactional(readOnly = true)
    public List<FileInfoDto> getUserFiles(User user, int limit) {
        log.debug("Получение файлов для пользователя: {}, limit: {}", user.getLogin(), limit);


        List<FileInfo> files = fileRepository.findByUser(user, PageRequest.of(0, limit));

        return files.stream()
                .map(file -> new FileInfoDto(file.getFilename(), file.getSize()))
                .collect(Collectors.toList());
    }


}