package ru.netology.cloudService.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RenameFileRequest(
        @NotBlank(message = "Новое имя файла не может быть пустым")
        @JsonProperty ("filename")
        String filename
) {
}