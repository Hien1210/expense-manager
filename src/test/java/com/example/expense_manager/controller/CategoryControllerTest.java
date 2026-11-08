package com.example.expense_manager.controller;

import com.example.expense_manager.dto.CategoryCreateRequest;
import com.example.expense_manager.dto.CategoryResponse;
import com.example.expense_manager.dto.RegisterRequest;
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
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

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
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void createsValidCategory() throws Exception {
        MockHttpSession session = registerAndLogin("cat-create@example.com");

        mockMvc.perform(post("/api/v1/categories")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CategoryCreateRequest("Salary", TransactionType.INCOME))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Salary"))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void trimsLeadingAndTrailingWhitespaceFromName() throws Exception {
        MockHttpSession session = registerAndLogin("cat-trim@example.com");

        mockMvc.perform(post("/api/v1/categories")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CategoryCreateRequest("  Rent  ", TransactionType.EXPENSE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rent"));
    }

    @Test
    void listsOnlyCurrentUsersCategories() throws Exception {
        MockHttpSession sessionA = registerAndLogin("cat-list-a@example.com");
        MockHttpSession sessionB = registerAndLogin("cat-list-b@example.com");

        createCategory(sessionA, "Salary", TransactionType.INCOME);
        createCategory(sessionA, "Rent", TransactionType.EXPENSE);
        createCategory(sessionB, "Bonus", TransactionType.INCOME);

        mockMvc.perform(get("/api/v1/categories").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.containsInAnyOrder("Salary", "Rent")));
    }

    @Test
    void updatesCategory() throws Exception {
        MockHttpSession session = registerAndLogin("cat-update@example.com");
        Long id = createCategory(session, "Groceries", TransactionType.EXPENSE);

        mockMvc.perform(put("/api/v1/categories/" + id)
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CategoryCreateRequest("Food", TransactionType.EXPENSE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Food"));
    }

    @Test
    void deletesUnusedCategory() throws Exception {
        MockHttpSession session = registerAndLogin("cat-delete@example.com");
        Long id = createCategory(session, "Temp", TransactionType.EXPENSE);

        mockMvc.perform(delete("/api/v1/categories/" + id).session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/categories/" + id).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void allowsTypeChangeWhenCategoryHasNoTransactions() throws Exception {
        MockHttpSession session = registerAndLogin("cat-type-no-tx@example.com");
        Long id = createCategory(session, "Freelance", TransactionType.EXPENSE);

        mockMvc.perform(put("/api/v1/categories/" + id)
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CategoryCreateRequest("Freelance", TransactionType.INCOME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    void allowsTypeChangeWhenAllExistingTransactionsAlreadyMatchNewType() throws Exception {
        MockHttpSession session = registerAndLogin("cat-type-matching-tx@example.com");
        Long id = createCategory(session, "Reclassified", TransactionType.EXPENSE);
        linkTransactionToCategory("cat-type-matching-tx@example.com", id, TransactionType.INCOME);

        mockMvc.perform(put("/api/v1/categories/" + id)
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CategoryCreateRequest("Reclassified", TransactionType.INCOME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    void rejectsTypeChangeWhenExistingTransactionsHaveDifferentType() throws Exception {
        MockHttpSession session = registerAndLogin("cat-type-conflict-tx@example.com");
        Long id = createCategory(session, "Groceries2", TransactionType.EXPENSE);
        linkTransactionToCategory("cat-type-conflict-tx@example.com", id, TransactionType.EXPENSE);

        mockMvc.perform(put("/api/v1/categories/" + id)
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CategoryCreateRequest("Groceries2", TransactionType.INCOME))))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsDuplicateNameWithinSameType() throws Exception {
        MockHttpSession session = registerAndLogin("cat-dup@example.com");
        createCategory(session, "Salary", TransactionType.INCOME);

        mockMvc.perform(post("/api/v1/categories")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CategoryCreateRequest("Salary", TransactionType.INCOME))))
                .andExpect(status().isConflict());
    }

    @Test
    void allowsSameNameWhenTypeDiffers() throws Exception {
        MockHttpSession session = registerAndLogin("cat-dup-type@example.com");
        createCategory(session, "Misc", TransactionType.INCOME);

        mockMvc.perform(post("/api/v1/categories")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CategoryCreateRequest("Misc", TransactionType.EXPENSE))))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsAccessToOtherUsersCategory() throws Exception {
        MockHttpSession ownerSession = registerAndLogin("cat-owner-get@example.com");
        MockHttpSession strangerSession = registerAndLogin("cat-stranger-get@example.com");
        Long id = createCategory(ownerSession, "Private", TransactionType.EXPENSE);

        mockMvc.perform(get("/api/v1/categories/" + id).session(strangerSession))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUpdateOfOtherUsersCategory() throws Exception {
        MockHttpSession ownerSession = registerAndLogin("cat-owner-update@example.com");
        MockHttpSession strangerSession = registerAndLogin("cat-stranger-update@example.com");
        Long id = createCategory(ownerSession, "Private", TransactionType.EXPENSE);

        mockMvc.perform(put("/api/v1/categories/" + id)
                        .session(strangerSession)
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CategoryCreateRequest("Hacked", TransactionType.EXPENSE))))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsDeleteOfOtherUsersCategory() throws Exception {
        MockHttpSession ownerSession = registerAndLogin("cat-owner-delete@example.com");
        MockHttpSession strangerSession = registerAndLogin("cat-stranger-delete@example.com");
        Long id = createCategory(ownerSession, "Private", TransactionType.EXPENSE);

        mockMvc.perform(delete("/api/v1/categories/" + id).session(strangerSession).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsDeleteOfCategoryUsedByTransaction() throws Exception {
        MockHttpSession session = registerAndLogin("cat-in-use@example.com");
        Long id = createCategory(session, "Used", TransactionType.EXPENSE);
        linkTransactionToCategory("cat-in-use@example.com", id, TransactionType.EXPENSE);

        mockMvc.perform(delete("/api/v1/categories/" + id).session(session).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidCategoryType() throws Exception {
        MockHttpSession session = registerAndLogin("cat-invalid-type@example.com");

        String invalidTypeJson = "{\"name\":\"Weird\",\"type\":\"NOT_A_TYPE\"}";

        mockMvc.perform(post("/api/v1/categories")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content(invalidTypeJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreateWithoutCsrfToken() throws Exception {
        MockHttpSession session = registerAndLogin("cat-no-csrf@example.com");

        mockMvc.perform(post("/api/v1/categories")
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CategoryCreateRequest("NoCsrf", TransactionType.EXPENSE))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsListWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
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
                        .content(objectMapper.writeValueAsString(
                                new com.example.expense_manager.dto.LoginRequest(email, "password123"))))
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

    private void linkTransactionToCategory(String ownerEmail, Long categoryId, TransactionType transactionType) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow();
        Category category = new Category();
        category.setId(categoryId);

        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("50.00"));
        transaction.setType(transactionType);
        transaction.setTransactionDate(LocalDate.now());
        transaction.setUser(owner);
        transaction.setCategory(category);
        transactionRepository.save(transaction);
    }
}
