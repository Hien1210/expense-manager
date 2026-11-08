package com.example.expense_manager.dto;

import com.example.expense_manager.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        TransactionType type,
        LocalDate transactionDate,
        Long categoryId,
        String categoryName
) {
}
