package ru.netology.cloudService.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameFileRequest(
        @NotBlank(message = "Новое имя файла не может быть пустым")
        String name
) {
}