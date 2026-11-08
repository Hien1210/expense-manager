package com.example.expense_manager.controller;

import com.example.expense_manager.dto.CategoryCreateRequest;
import com.example.expense_manager.dto.CategoryResponse;
import com.example.expense_manager.dto.LoginRequest;
import com.example.expense_manager.dto.RegisterRequest;
import com.example.expense_manager.dto.TransactionCreateRequest;
import com.example.expense_manager.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.example.expense_manager.repository.UserRepository userRepository;

    @Test
    void summaryReturnsZerosWhenNoTransactions() throws Exception {
        MockHttpSession session = registerAndLogin("dash-empty@example.com");

        mockMvc.perform(get("/api/v1/dashboard/summary").session(session).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-11"))
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.netCashflow").value(0));
    }

    @Test
    void summaryComputesTotalsForGivenMonth() throws Exception {
        MockHttpSession session = registerAndLogin("dash-summary@example.com");
        Long incomeCategory = createCategory(session, "Salary", TransactionType.INCOME);
        Long expenseCategory = createCategory(session, "Rent", TransactionType.EXPENSE);

        createTransaction(session, new BigDecimal("1000.00"), TransactionType.INCOME, LocalDate.of(2026, 11, 1), incomeCategory);
        createTransaction(session, new BigDecimal("300.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 15), expenseCategory);

        mockMvc.perform(get("/api/v1/dashboard/summary").session(session).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.totalExpense").value(300.00))
                .andExpect(jsonPath("$.netCashflow").value(700.00));
    }

    @Test
    void summaryIgnoresTransactionsFromOtherMonths() throws Exception {
        MockHttpSession session = registerAndLogin("dash-other-month@example.com");
        Long categoryId = createCategory(session, "Misc", TransactionType.EXPENSE);

        createTransaction(session, new BigDecimal("50.00"), TransactionType.EXPENSE, LocalDate.of(2026, 10, 20), categoryId);

        mockMvc.perform(get("/api/v1/dashboard/summary").session(session).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(0));
    }

    @Test
    void summaryDefaultsToCurrentMonthWhenNotSpecified() throws Exception {
        MockHttpSession session = registerAndLogin("dash-default-month@example.com");

        String expectedMonth = java.time.YearMonth.now().toString();

        mockMvc.perform(get("/api/v1/dashboard/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(expectedMonth));
    }

    @Test
    void summaryIsScopedToCurrentUserOnly() throws Exception {
        MockHttpSession sessionA = registerAndLogin("dash-user-a@example.com");
        MockHttpSession sessionB = registerAndLogin("dash-user-b@example.com");
        Long categoryA = createCategory(sessionA, "Salary", TransactionType.INCOME);
        Long categoryB = createCategory(sessionB, "Salary", TransactionType.INCOME);

        createTransaction(sessionA, new BigDecimal("100.00"), TransactionType.INCOME, LocalDate.of(2026, 11, 1), categoryA);
        createTransaction(sessionB, new BigDecimal("99999.00"), TransactionType.INCOME, LocalDate.of(2026, 11, 1), categoryB);

        mockMvc.perform(get("/api/v1/dashboard/summary").session(sessionA).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(100.00));
    }

    @Test
    void summaryRejectsInvalidMonthFormat() throws Exception {
        MockHttpSession session = registerAndLogin("dash-bad-month@example.com");

        mockMvc.perform(get("/api/v1/dashboard/summary").session(session).param("month", "not-a-month"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void categoriesReturnsEmptyListsWhenNoTransactions() throws Exception {
        MockHttpSession session = registerAndLogin("dash-cat-empty@example.com");

        mockMvc.perform(get("/api/v1/dashboard/categories").session(session).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income.length()").value(0))
                .andExpect(jsonPath("$.expense.length()").value(0));
    }

    @Test
    void categoriesGroupsAndSeparatesIncomeAndExpense() throws Exception {
        MockHttpSession session = registerAndLogin("dash-cat-group@example.com");
        Long salary = createCategory(session, "Salary", TransactionType.INCOME);
        Long groceries = createCategory(session, "Groceries", TransactionType.EXPENSE);
        Long rent = createCategory(session, "Rent", TransactionType.EXPENSE);

        createTransaction(session, new BigDecimal("2000.00"), TransactionType.INCOME, LocalDate.of(2026, 11, 1), salary);
        createTransaction(session, new BigDecimal("150.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 2), groceries);
        createTransaction(session, new BigDecimal("100.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 3), groceries);
        createTransaction(session, new BigDecimal("800.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 4), rent);

        mockMvc.perform(get("/api/v1/dashboard/categories").session(session).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income.length()").value(1))
                .andExpect(jsonPath("$.income[0].categoryName").value("Salary"))
                .andExpect(jsonPath("$.income[0].totalAmount").value(2000.00))
                .andExpect(jsonPath("$.expense.length()").value(2))
                // ordered by total amount DESC: Rent (800.00) before Groceries (150+100=250.00)
                .andExpect(jsonPath("$.expense[0].categoryName").value("Rent"))
                .andExpect(jsonPath("$.expense[0].totalAmount").value(800.00))
                .andExpect(jsonPath("$.expense[1].categoryName").value("Groceries"))
                .andExpect(jsonPath("$.expense[1].totalAmount").value(250.00));
    }

    @Test
    void categoriesOmitsCategoriesWithoutTransactionsThatMonth() throws Exception {
        MockHttpSession session = registerAndLogin("dash-cat-omit@example.com");
        createCategory(session, "UnusedCategory", TransactionType.EXPENSE);

        mockMvc.perform(get("/api/v1/dashboard/categories").session(session).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expense.length()").value(0));
    }

    @Test
    void categoriesIsScopedToCurrentUserOnly() throws Exception {
        MockHttpSession sessionA = registerAndLogin("dash-cat-user-a@example.com");
        MockHttpSession sessionB = registerAndLogin("dash-cat-user-b@example.com");
        Long categoryA = createCategory(sessionA, "Groceries", TransactionType.EXPENSE);
        Long categoryB = createCategory(sessionB, "Groceries", TransactionType.EXPENSE);

        createTransaction(sessionA, new BigDecimal("10.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), categoryA);
        createTransaction(sessionB, new BigDecimal("999.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), categoryB);

        mockMvc.perform(get("/api/v1/dashboard/categories").session(sessionA).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expense.length()").value(1))
                .andExpect(jsonPath("$.expense[0].totalAmount").value(10.00));
    }

    @Test
    void monthlyReturnsOldestToNewestWithZerosForMissingMonths() throws Exception {
        MockHttpSession session = registerAndLogin("dash-monthly@example.com");
        Long categoryId = createCategory(session, "Salary", TransactionType.INCOME);

        createTransaction(session, new BigDecimal("500.00"), TransactionType.INCOME, LocalDate.of(2026, 9, 1), categoryId);
        // 2026-08 and 2026-10 deliberately left with no transactions.
        createTransaction(session, new BigDecimal("700.00"), TransactionType.INCOME, LocalDate.of(2026, 11, 1), categoryId);

        mockMvc.perform(get("/api/v1/dashboard/monthly").session(session)
                        .param("month", "2026-11").param("months", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].month").value("2026-08"))
                .andExpect(jsonPath("$[0].totalIncome").value(0))
                .andExpect(jsonPath("$[1].month").value("2026-09"))
                .andExpect(jsonPath("$[1].totalIncome").value(500.00))
                .andExpect(jsonPath("$[2].month").value("2026-10"))
                .andExpect(jsonPath("$[2].totalIncome").value(0))
                .andExpect(jsonPath("$[3].month").value("2026-11"))
                .andExpect(jsonPath("$[3].totalIncome").value(700.00));
    }

    @Test
    void monthlyDefaultsToSixMonthsEndingAtGivenMonth() throws Exception {
        MockHttpSession session = registerAndLogin("dash-monthly-default@example.com");

        mockMvc.perform(get("/api/v1/dashboard/monthly").session(session).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].month").value("2026-06"))
                .andExpect(jsonPath("$[5].month").value("2026-11"));
    }

    @Test
    void monthlyRejectsMonthsOutOfRange() throws Exception {
        MockHttpSession session = registerAndLogin("dash-monthly-range@example.com");

        mockMvc.perform(get("/api/v1/dashboard/monthly").session(session).param("months", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/dashboard/monthly").session(session).param("months", "25"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void monthlyIsScopedToCurrentUserOnly() throws Exception {
        MockHttpSession sessionA = registerAndLogin("dash-monthly-user-a@example.com");
        MockHttpSession sessionB = registerAndLogin("dash-monthly-user-b@example.com");
        Long categoryA = createCategory(sessionA, "Salary", TransactionType.INCOME);
        Long categoryB = createCategory(sessionB, "Salary", TransactionType.INCOME);

        createTransaction(sessionA, new BigDecimal("10.00"), TransactionType.INCOME, LocalDate.of(2026, 11, 1), categoryA);
        createTransaction(sessionB, new BigDecimal("9999.00"), TransactionType.INCOME, LocalDate.of(2026, 11, 1), categoryB);

        mockMvc.perform(get("/api/v1/dashboard/monthly").session(sessionA)
                        .param("month", "2026-11").param("months", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalIncome").value(10.00));
    }

    @Test
    void recentTransactionsReturnsMostRecentFirstWithDefaultLimit() throws Exception {
        MockHttpSession session = registerAndLogin("dash-recent@example.com");
        Long categoryId = createCategory(session, "Misc", TransactionType.EXPENSE);

        Long older = createTransaction(session, new BigDecimal("1.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), categoryId);
        Long newer = createTransaction(session, new BigDecimal("2.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 5), categoryId);

        mockMvc.perform(get("/api/v1/dashboard/recent-transactions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(newer))
                .andExpect(jsonPath("$[1].id").value(older));
    }

    @Test
    void recentTransactionsRespectsCustomLimit() throws Exception {
        MockHttpSession session = registerAndLogin("dash-recent-limit@example.com");
        Long categoryId = createCategory(session, "Misc", TransactionType.EXPENSE);

        for (int i = 0; i < 5; i++) {
            createTransaction(session, new BigDecimal("1.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 1 + i), categoryId);
        }

        mockMvc.perform(get("/api/v1/dashboard/recent-transactions").session(session).param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void recentTransactionsRejectsLimitAboveMaximum() throws Exception {
        MockHttpSession session = registerAndLogin("dash-recent-max@example.com");

        mockMvc.perform(get("/api/v1/dashboard/recent-transactions").session(session).param("limit", "51"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recentTransactionsRejectsNonNumericLimitWithoutServerError() throws Exception {
        MockHttpSession session = registerAndLogin("dash-recent-bad-limit@example.com");

        mockMvc.perform(get("/api/v1/dashboard/recent-transactions").session(session).param("limit", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recentTransactionsReturnsEmptyWhenNoTransactions() throws Exception {
        MockHttpSession session = registerAndLogin("dash-recent-empty@example.com");

        mockMvc.perform(get("/api/v1/dashboard/recent-transactions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void recentTransactionsIsScopedToCurrentUserOnly() throws Exception {
        MockHttpSession sessionA = registerAndLogin("dash-recent-user-a@example.com");
        MockHttpSession sessionB = registerAndLogin("dash-recent-user-b@example.com");
        Long categoryA = createCategory(sessionA, "Misc", TransactionType.EXPENSE);
        Long categoryB = createCategory(sessionB, "Misc", TransactionType.EXPENSE);

        createTransaction(sessionA, new BigDecimal("1.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), categoryA);
        createTransaction(sessionB, new BigDecimal("2.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), categoryB);

        mockMvc.perform(get("/api/v1/dashboard/recent-transactions").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amount").value(1.00));
    }

    @Test
    void rejectsUnauthenticatedAccessToAllDashboardEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/categories")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/monthly")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/recent-transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void preservesDecimalPrecisionWithCentValues() throws Exception {
        MockHttpSession session = registerAndLogin("dash-precision@example.com");
        Long categoryId = createCategory(session, "Misc", TransactionType.EXPENSE);

        createTransaction(session, new BigDecimal("19.99"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), categoryId);
        createTransaction(session, new BigDecimal("0.01"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 2), categoryId);

        mockMvc.perform(get("/api/v1/dashboard/summary").session(session).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(20.00))
                .andExpect(jsonPath("$.netCashflow").value(-20.00));
    }

    private MockHttpSession registerAndLogin(String email) throws Exception {
        RegisterRequest register = new RegisterRequest(email, "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        userRepository.findByEmail(email).ifPresent(u -> {
            u.setEnabled(true);
            userRepository.save(u);
        });

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Long createCategory(MockHttpSession session, String name, TransactionType type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CategoryCreateRequest(name, type))))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), CategoryResponse.class);
        return response.id();
    }

    private Long createTransaction(MockHttpSession session, BigDecimal amount, TransactionType type,
                                    LocalDate date, Long categoryId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TransactionCreateRequest(amount, type, date, categoryId))))
                .andExpect(status().isCreated())
                .andReturn();

        com.example.expense_manager.dto.TransactionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), com.example.expense_manager.dto.TransactionResponse.class);
        return response.id();
    }
}
