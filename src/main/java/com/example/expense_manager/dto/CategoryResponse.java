package com.example.expense_manager.dto;

import com.example.expense_manager.entity.TransactionType;

public record CategoryResponse(Long id, String name, TransactionType type) {
}
