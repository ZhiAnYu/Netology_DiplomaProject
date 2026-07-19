package ru.netology.cloudService.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.netology.cloudService.dto.FileInfoDto;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.service.FileService;

import java.util.List;

@RestController
@RequestMapping("/")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileInfoDto>> getList(
            @RequestHeader("auth-token") String token,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {

        log.debug("Запрос списка файлов. limit={}", limit);

        User currentUser = (User) request.getAttribute("currentUser");

        List<FileInfoDto> files = fileService.getUserFiles(currentUser, limit);

        return ResponseEntity.ok(files);
    }
}