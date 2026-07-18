package ru.netology.cloudService.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(@JsonProperty("auth-token") String token) {
}
