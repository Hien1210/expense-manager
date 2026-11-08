package com.example.expense_manager.controller;

import com.example.expense_manager.dto.CategoryCreateRequest;
import com.example.expense_manager.dto.CategoryResponse;
import com.example.expense_manager.dto.LoginRequest;
import com.example.expense_manager.dto.RegisterRequest;
import com.example.expense_manager.dto.TransactionCreateRequest;
import com.example.expense_manager.dto.TransactionResponse;
import com.example.expense_manager.entity.Category;
import com.example.expense_manager.entity.Transaction;
import com.example.expense_manager.entity.TransactionType;
import com.example.expense_manager.entity.User;
import com.example.expense_manager.repository.TransactionRepository;
import com.example.expense_manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void createsValidTransaction() throws Exception {
        MockHttpSession session = registerAndLogin("tx-create@example.com");
        Long categoryId = createCategory(session, "Salary", TransactionType.INCOME);

        mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TransactionCreateRequest(
                                new BigDecimal("1500.00"), TransactionType.INCOME, LocalDate.of(2026, 11, 5), categoryId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("Salary"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void rejectsNonPositiveAmount() throws Exception {
        MockHttpSession session = registerAndLogin("tx-neg-amount@example.com");
        Long categoryId = createCategory(session, "Rent", TransactionType.EXPENSE);

        mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TransactionCreateRequest(
                                BigDecimal.ZERO, TransactionType.EXPENSE, LocalDate.now(), categoryId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidTransactionType() throws Exception {
        MockHttpSession session = registerAndLogin("tx-bad-type@example.com");
        Long categoryId = createCategory(session, "Rent", TransactionType.EXPENSE);

        String invalidJson = "{\"amount\":50.00,\"type\":\"NOT_A_TYPE\",\"transactionDate\":\"2026-11-01\",\"categoryId\":"
                + categoryId + "}";

        mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsWhenCategoryDoesNotExist() throws Exception {
        MockHttpSession session = registerAndLogin("tx-no-category@example.com");

        mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TransactionCreateRequest(
                                new BigDecimal("10.00"), TransactionType.EXPENSE, LocalDate.now(), 999999999L))))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsWhenCategoryBelongsToOtherUser() throws Exception {
        MockHttpSession ownerSession = registerAndLogin("tx-cat-owner@example.com");
        MockHttpSession strangerSession = registerAndLogin("tx-cat-stranger@example.com");
        Long categoryId = createCategory(ownerSession, "Private", TransactionType.EXPENSE);

        mockMvc.perform(post("/api/v1/transactions")
                        .session(strangerSession)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TransactionCreateRequest(
                                new BigDecimal("10.00"), TransactionType.EXPENSE, LocalDate.now(), categoryId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsWhenCategoryTypeDoesNotMatchTransactionType() throws Exception {
        MockHttpSession session = registerAndLogin("tx-type-mismatch@example.com");
        Long categoryId = createCategory(session, "Salary", TransactionType.INCOME);

        mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TransactionCreateRequest(
                                new BigDecimal("10.00"), TransactionType.EXPENSE, LocalDate.now(), categoryId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesTransaction() throws Exception {
        MockHttpSession session = registerAndLogin("tx-update@example.com");
        Long categoryId = createCategory(session, "Groceries", TransactionType.EXPENSE);
        Long id = createTransaction(session, new BigDecimal("40.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), categoryId);

        mockMvc.perform(put("/api/v1/transactions/" + id)
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TransactionCreateRequest(
                                new BigDecimal("55.50"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 2), categoryId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(55.50))
                .andExpect(jsonPath("$.transactionDate").value("2026-11-02"));
    }

    @Test
    void deletesTransaction() throws Exception {
        MockHttpSession session = registerAndLogin("tx-delete@example.com");
        Long categoryId = createCategory(session, "Temp", TransactionType.EXPENSE);
        Long id = createTransaction(session, new BigDecimal("20.00"), TransactionType.EXPENSE, LocalDate.now(), categoryId);

        mockMvc.perform(delete("/api/v1/transactions/" + id).session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/transactions/" + id).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAccessToOtherUsersTransaction() throws Exception {
        MockHttpSession ownerSession = registerAndLogin("tx-owner-get@example.com");
        MockHttpSession strangerSession = registerAndLogin("tx-stranger-get@example.com");
        Long categoryId = createCategory(ownerSession, "Private", TransactionType.EXPENSE);
        Long id = createTransaction(ownerSession, new BigDecimal("30.00"), TransactionType.EXPENSE, LocalDate.now(), categoryId);

        mockMvc.perform(get("/api/v1/transactions/" + id).session(strangerSession))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUpdateOfOtherUsersTransaction() throws Exception {
        MockHttpSession ownerSession = registerAndLogin("tx-owner-update@example.com");
        MockHttpSession strangerSession = registerAndLogin("tx-stranger-update@example.com");
        Long categoryId = createCategory(ownerSession, "Private", TransactionType.EXPENSE);
        Long id = createTransaction(ownerSession, new BigDecimal("30.00"), TransactionType.EXPENSE, LocalDate.now(), categoryId);

        mockMvc.perform(put("/api/v1/transactions/" + id)
                        .session(strangerSession)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TransactionCreateRequest(
                                new BigDecimal("999.00"), TransactionType.EXPENSE, LocalDate.now(), categoryId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsDeleteOfOtherUsersTransaction() throws Exception {
        MockHttpSession ownerSession = registerAndLogin("tx-owner-delete@example.com");
        MockHttpSession strangerSession = registerAndLogin("tx-stranger-delete@example.com");
        Long categoryId = createCategory(ownerSession, "Private", TransactionType.EXPENSE);
        Long id = createTransaction(ownerSession, new BigDecimal("30.00"), TransactionType.EXPENSE, LocalDate.now(), categoryId);

        mockMvc.perform(delete("/api/v1/transactions/" + id).session(strangerSession).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsOnlyCurrentUsersTransactions() throws Exception {
        MockHttpSession sessionA = registerAndLogin("tx-list-a@example.com");
        MockHttpSession sessionB = registerAndLogin("tx-list-b@example.com");
        Long categoryA = createCategory(sessionA, "Salary", TransactionType.INCOME);
        Long categoryB = createCategory(sessionB, "Salary", TransactionType.INCOME);

        createTransaction(sessionA, new BigDecimal("100.00"), TransactionType.INCOME, LocalDate.of(2026, 11, 1), categoryA);
        createTransaction(sessionB, new BigDecimal("200.00"), TransactionType.INCOME, LocalDate.of(2026, 11, 1), categoryB);

        mockMvc.perform(get("/api/v1/transactions").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(100.00));
    }

    @Test
    void filtersByMonth() throws Exception {
        MockHttpSession session = registerAndLogin("tx-filter-month@example.com");
        Long categoryId = createCategory(session, "Misc", TransactionType.EXPENSE);

        createTransaction(session, new BigDecimal("10.00"), TransactionType.EXPENSE, LocalDate.of(2026, 10, 15), categoryId);
        createTransaction(session, new BigDecimal("20.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 15), categoryId);

        mockMvc.perform(get("/api/v1/transactions").session(session).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(20.00));
    }

    @Test
    void filtersByType() throws Exception {
        MockHttpSession session = registerAndLogin("tx-filter-type@example.com");
        Long incomeCategory = createCategory(session, "Salary", TransactionType.INCOME);
        Long expenseCategory = createCategory(session, "Rent", TransactionType.EXPENSE);

        createTransaction(session, new BigDecimal("1000.00"), TransactionType.INCOME, LocalDate.of(2026, 11, 1), incomeCategory);
        createTransaction(session, new BigDecimal("300.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), expenseCategory);

        mockMvc.perform(get("/api/v1/transactions").session(session).param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("EXPENSE"));
    }

    @Test
    void filtersByCategory() throws Exception {
        MockHttpSession session = registerAndLogin("tx-filter-category@example.com");
        Long categoryOne = createCategory(session, "Groceries", TransactionType.EXPENSE);
        Long categoryTwo = createCategory(session, "Transport", TransactionType.EXPENSE);

        createTransaction(session, new BigDecimal("50.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), categoryOne);
        createTransaction(session, new BigDecimal("15.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), categoryTwo);

        mockMvc.perform(get("/api/v1/transactions").session(session).param("categoryId", String.valueOf(categoryOne)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].categoryId").value(categoryOne));
    }

    @Test
    void combinesMonthTypeAndCategoryFilters() throws Exception {
        MockHttpSession session = registerAndLogin("tx-filter-combo@example.com");
        Long expenseCategory = createCategory(session, "Groceries", TransactionType.EXPENSE);
        Long incomeCategory = createCategory(session, "Salary", TransactionType.INCOME);

        // Matches all filters
        createTransaction(session, new BigDecimal("50.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 10), expenseCategory);
        // Wrong month
        createTransaction(session, new BigDecimal("60.00"), TransactionType.EXPENSE, LocalDate.of(2026, 10, 10), expenseCategory);
        // Wrong type/category
        createTransaction(session, new BigDecimal("1000.00"), TransactionType.INCOME, LocalDate.of(2026, 11, 10), incomeCategory);

        mockMvc.perform(get("/api/v1/transactions").session(session)
                        .param("month", "2026-11")
                        .param("type", "EXPENSE")
                        .param("categoryId", String.valueOf(expenseCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(50.00));
    }

    @Test
    void appliesDefaultSortOrderWithIdTiebreaker() throws Exception {
        MockHttpSession session = registerAndLogin("tx-sort-default@example.com");
        Long categoryId = createCategory(session, "Misc", TransactionType.EXPENSE);

        Long older = createTransaction(session, new BigDecimal("10.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), categoryId);
        Long sameDateEarlier = createTransaction(session, new BigDecimal("20.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 5), categoryId);
        Long sameDateLater = createTransaction(session, new BigDecimal("30.00"), TransactionType.EXPENSE, LocalDate.of(2026, 11, 5), categoryId);

        mockMvc.perform(get("/api/v1/transactions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sameDateLater))
                .andExpect(jsonPath("$.content[1].id").value(sameDateEarlier))
                .andExpect(jsonPath("$.content[2].id").value(older))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void capsPageSizeAtMaximum() throws Exception {
        MockHttpSession session = registerAndLogin("tx-page-cap@example.com");
        Long categoryId = createCategory(session, "Misc", TransactionType.EXPENSE);
        createTransaction(session, new BigDecimal("10.00"), TransactionType.EXPENSE, LocalDate.now(), categoryId);

        mockMvc.perform(get("/api/v1/transactions").session(session).param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void paginatesAcrossMultiplePagesPreservingOrderAndNoDuplicates() throws Exception {
        MockHttpSession session = registerAndLogin("tx-page-multi@example.com");
        Long categoryId = createCategory(session, "Misc", TransactionType.EXPENSE);

        // 45 transactions, each with a distinct, strictly decreasing transactionDate,
        // so the global sort order (transactionDate DESC) exactly matches creation order.
        List<Long> expectedOrder = createTransactionsOrderedByDateDesc(
                "tx-page-multi@example.com", categoryId, 45, LocalDate.of(2026, 12, 31));

        MvcResult page0Result = mockMvc.perform(get("/api/v1/transactions").session(session)
                        .param("size", "20").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(45))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.content.length()").value(20))
                .andReturn();

        MvcResult page1Result = mockMvc.perform(get("/api/v1/transactions").session(session)
                        .param("size", "20").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(45))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content.length()").value(20))
                .andReturn();

        MvcResult page2Result = mockMvc.perform(get("/api/v1/transactions").session(session)
                        .param("size", "20").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(45))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.content.length()").value(5))
                .andReturn();

        List<Long> page0Ids = extractContentIds(page0Result);
        List<Long> page1Ids = extractContentIds(page1Result);
        List<Long> page2Ids = extractContentIds(page2Result);

        // Order within and across pages must follow transactionDate DESC (id DESC as tiebreaker,
        // already verified separately in appliesDefaultSortOrderWithIdTiebreaker).
        assertThat(page0Ids).isEqualTo(expectedOrder.subList(0, 20));
        assertThat(page1Ids).isEqualTo(expectedOrder.subList(20, 40));
        assertThat(page2Ids).isEqualTo(expectedOrder.subList(40, 45));

        // No duplicates and no gaps across the three pages.
        Set<Long> allIds = new HashSet<>();
        allIds.addAll(page0Ids);
        allIds.addAll(page1Ids);
        allIds.addAll(page2Ids);
        assertThat(allIds).hasSize(45);
        assertThat(allIds).containsExactlyInAnyOrderElementsOf(expectedOrder);
    }

    @Test
    void returnsEmptyValidPageWhenPageExceedsAvailableData() throws Exception {
        MockHttpSession session = registerAndLogin("tx-page-overflow@example.com");
        Long categoryId = createCategory(session, "Misc", TransactionType.EXPENSE);

        createTransactionsOrderedByDateDesc(
                "tx-page-overflow@example.com", categoryId, 45, LocalDate.of(2026, 12, 31));

        mockMvc.perform(get("/api/v1/transactions").session(session)
                        .param("size", "20").param("page", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.numberOfElements").value(0))
                .andExpect(jsonPath("$.totalElements").value(45))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(10));
    }

    @Test
    void rejectsInvalidSortField() throws Exception {
        MockHttpSession session = registerAndLogin("tx-bad-sort@example.com");

        mockMvc.perform(get("/api/v1/transactions").session(session).param("sort", "notAField,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreateWithoutCsrfToken() throws Exception {
        MockHttpSession session = registerAndLogin("tx-no-csrf@example.com");
        Long categoryId = createCategory(session, "Misc", TransactionType.EXPENSE);

        mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TransactionCreateRequest(
                                new BigDecimal("10.00"), TransactionType.EXPENSE, LocalDate.now(), categoryId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsListWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized());
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

        TransactionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), TransactionResponse.class);
        return response.id();
    }

    /**
     * Inserts {@code count} transactions directly via the repository (bypassing the HTTP layer
     * for speed), each with a distinct transactionDate strictly decreasing by one day starting
     * at {@code startDate}. The returned list is therefore already in the exact order the API
     * must return them in under the default sort (transactionDate DESC).
     */
    private List<Long> createTransactionsOrderedByDateDesc(String ownerEmail, Long categoryId,
                                                             int count, LocalDate startDate) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow();
        Category categoryRef = new Category();
        categoryRef.setId(categoryId);

        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Transaction transaction = new Transaction();
            transaction.setAmount(BigDecimal.ONE);
            transaction.setType(TransactionType.EXPENSE);
            transaction.setTransactionDate(startDate.minusDays(i));
            transaction.setUser(owner);
            transaction.setCategory(categoryRef);
            ids.add(transactionRepository.save(transaction).getId());
        }
        return ids;
    }

    private List<Long> extractContentIds(MvcResult result) throws Exception {
        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        List<Long> ids = new ArrayList<>();
        for (JsonNode node : content) {
            ids.add(node.get("id").asLong());
        }
        return ids;
    }
}
