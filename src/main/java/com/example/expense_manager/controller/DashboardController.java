package com.example.expense_manager.controller;

import com.example.expense_manager.dto.DashboardCategoriesResponse;
import com.example.expense_manager.dto.DashboardSummaryResponse;
import com.example.expense_manager.dto.MonthlyOverviewItem;
import com.example.expense_manager.dto.TransactionResponse;
import com.example.expense_manager.security.UserPrincipal;
import com.example.expense_manager.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(@RequestParam(required = false) String month,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.summary(principal.getUser(), month);
    }

    @GetMapping("/categories")
    public DashboardCategoriesResponse categories(@RequestParam(required = false) String month,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.categories(principal.getUser(), month);
    }

    @GetMapping("/monthly")
    public List<MonthlyOverviewItem> monthly(@RequestParam(required = false) String month,
                                              @RequestParam(required = false) Integer months,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.monthly(principal.getUser(), month, months);
    }

    @GetMapping("/recent-transactions")
    public List<TransactionResponse> recentTransactions(@RequestParam(required = false) Integer limit,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.recentTransactions(principal.getUser(), limit);
    }
}
