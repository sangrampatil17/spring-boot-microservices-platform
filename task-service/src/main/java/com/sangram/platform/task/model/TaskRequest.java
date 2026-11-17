package com.sangram.platform.task.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TaskRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 2000) String description,
        TaskStatus status,
        List<String> tags) {
}
