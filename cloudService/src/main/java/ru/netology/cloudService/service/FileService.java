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
    public List<FileInfoDto> getUserFiles(User user, int limit) {
        log.debug("Получение файлов для пользователя: {}, limit: {}", user.getLogin(), limit);


        List<FileInfo> files = fileRepository.findByUser(user, PageRequest.of(0, limit));

        return files.stream()
                .map(file -> new FileInfoDto(file.getFilename(), file.getSize()))
                .collect(Collectors.toList());
    }
}