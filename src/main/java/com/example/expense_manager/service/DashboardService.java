package com.example.expense_manager.service;

import com.example.expense_manager.dto.CategoryTotalResponse;
import com.example.expense_manager.dto.DashboardCategoriesResponse;
import com.example.expense_manager.dto.DashboardSummaryResponse;
import com.example.expense_manager.dto.MonthlyOverviewItem;
import com.example.expense_manager.dto.TransactionResponse;
import com.example.expense_manager.entity.Category;
import com.example.expense_manager.entity.Transaction;
import com.example.expense_manager.entity.TransactionType;
import com.example.expense_manager.entity.User;
import com.example.expense_manager.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private static final int DEFAULT_MONTHLY_MONTHS = 6;
    private static final int MIN_MONTHLY_MONTHS = 1;
    private static final int MAX_MONTHLY_MONTHS = 24;
    private static final int DEFAULT_RECENT_LIMIT = 10;
    private static final int MAX_RECENT_LIMIT = 50;

    private final TransactionRepository transactionRepository;

    public DashboardService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public DashboardSummaryResponse summary(User owner, String month) {
        return summaryFor(owner, parseMonthOrDefault(month));
    }

    public DashboardCategoriesResponse categories(User owner, String month) {
        YearMonth yearMonth = parseMonthOrDefault(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1);

        List<CategoryTotalResponse> income = transactionRepository
                .sumAmountByCategoryForUserAndTypeAndDateRange(owner, TransactionType.INCOME, startDate, endDate);
        List<CategoryTotalResponse> expense = transactionRepository
                .sumAmountByCategoryForUserAndTypeAndDateRange(owner, TransactionType.EXPENSE, startDate, endDate);

        return new DashboardCategoriesResponse(yearMonth.toString(), income, expense);
    }

    public List<MonthlyOverviewItem> monthly(User owner, String month, Integer months) {
        YearMonth endMonth = parseMonthOrDefault(month);
        int count = normalizeMonths(months);
        YearMonth startMonth = endMonth.minusMonths(count - 1L);

        List<MonthlyOverviewItem> result = new ArrayList<>();
        for (YearMonth ym = startMonth; !ym.isAfter(endMonth); ym = ym.plusMonths(1)) {
            DashboardSummaryResponse monthSummary = summaryFor(owner, ym);
            result.add(new MonthlyOverviewItem(
                    monthSummary.month(), monthSummary.totalIncome(), monthSummary.totalExpense(), monthSummary.netCashflow()));
        }
        return result;
    }

    public List<TransactionResponse> recentTransactions(User owner, Integer limit) {
        int size = normalizeLimit(limit);
        Pageable pageable = PageRequest.of(0, size,
                Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id")));

        return transactionRepository.search(owner, null, null, null, null, pageable)
                .map(this::toTransactionResponse)
                .getContent();
    }

    private DashboardSummaryResponse summaryFor(User owner, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1);

        BigDecimal income = transactionRepository
                .sumAmountByUserAndTypeAndDateRange(owner, TransactionType.INCOME, startDate, endDate);
        BigDecimal expense = transactionRepository
                .sumAmountByUserAndTypeAndDateRange(owner, TransactionType.EXPENSE, startDate, endDate);

        return new DashboardSummaryResponse(yearMonth.toString(), income, expense, income.subtract(expense));
    }

    private YearMonth parseMonthOrDefault(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid month format, expected yyyy-MM");
        }
    }

    private int normalizeMonths(Integer months) {
        int value = months != null ? months : DEFAULT_MONTHLY_MONTHS;
        if (value < MIN_MONTHLY_MONTHS || value > MAX_MONTHLY_MONTHS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "months must be between " + MIN_MONTHLY_MONTHS + " and " + MAX_MONTHLY_MONTHS);
        }
        return value;
    }

    private int normalizeLimit(Integer limit) {
        int value = limit != null ? limit : DEFAULT_RECENT_LIMIT;
        if (value < 1 || value > MAX_RECENT_LIMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "limit must be between 1 and " + MAX_RECENT_LIMIT);
        }
        return value;
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        Category category = transaction.getCategory();
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getTransactionDate(),
                category.getId(),
                category.getName()
        );
    }
}
