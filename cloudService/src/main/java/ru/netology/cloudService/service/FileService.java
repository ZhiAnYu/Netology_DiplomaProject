package ru.netology.cloudService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.netology.cloudService.dto.FileInfoDto;
import ru.netology.cloudService.entity.FileInfo;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.repository.FileRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private final FileRepository fileRepository;

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
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