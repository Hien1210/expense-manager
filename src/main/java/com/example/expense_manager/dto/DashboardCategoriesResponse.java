package com.example.expense_manager.dto;

import java.util.List;

public record DashboardCategoriesResponse(
        String month,
        List<CategoryTotalResponse> income,
        List<CategoryTotalResponse> expense
) {
}
