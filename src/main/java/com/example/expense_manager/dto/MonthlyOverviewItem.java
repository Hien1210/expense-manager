package com.example.expense_manager.dto;

import java.math.BigDecimal;

public record MonthlyOverviewItem(
        String month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netCashflow
) {
}
