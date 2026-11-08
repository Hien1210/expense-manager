package com.example.expense_manager.dto;

import com.example.expense_manager.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionUpdateRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @NotNull TransactionType type,
        @NotNull LocalDate transactionDate,
        @NotNull Long categoryId
) {
}
