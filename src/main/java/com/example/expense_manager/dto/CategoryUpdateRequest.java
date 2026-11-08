package com.example.expense_manager.dto;

import com.example.expense_manager.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryUpdateRequest(
        @NotBlank String name,
        @NotNull TransactionType type
) {
}
