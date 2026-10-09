package com.workhub.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStatusRequest(
        @NotBlank(message = "Status name is required")
        @Size(max = 50, message = "Status name cannot exceed 50 characters")
        String name
) {}
