package com.example.expense_manager.dto;

import java.math.BigDecimal;

public record CategoryTotalResponse(
        Long categoryId,
        String categoryName,
        BigDecimal totalAmount
) {
}
