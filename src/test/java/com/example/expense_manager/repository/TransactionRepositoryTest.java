package com.example.expense_manager.repository;

import com.example.expense_manager.entity.Category;
import com.example.expense_manager.entity.Transaction;
import com.example.expense_manager.entity.TransactionType;
import com.example.expense_manager.entity.User;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hash");
        return userRepository.save(user);
    }

    private Category persistCategory(User owner, TransactionType type) {
        Category category = new Category();
        category.setName("Category-" + type);
        category.setType(type);
        category.setUser(owner);
        return categoryRepository.save(category);
    }

    @Test
    void savesAndReadsTransaction() {
        User owner = persistUser("tx-owner@example.com");
        Category category = persistCategory(owner, TransactionType.EXPENSE);

        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("150.00"));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setTransactionDate(LocalDate.now());
        transaction.setUser(owner);
        transaction.setCategory(category);

        Transaction saved = transactionRepository.save(transaction);

        assertThat(saved.getId()).isNotNull();
        assertThat(transactionRepository.findByIdAndUser(saved.getId(), owner)).isPresent();
    }

    @Test
    void rejectsNonPositiveAmount() {
        User owner = persistUser("tx-invalid@example.com");
        Category category = persistCategory(owner, TransactionType.EXPENSE);

        Transaction transaction = new Transaction();
        transaction.setAmount(BigDecimal.ZERO);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setTransactionDate(LocalDate.now());
        transaction.setUser(owner);
        transaction.setCategory(category);

        assertThrows(ConstraintViolationException.class, () -> transactionRepository.saveAndFlush(transaction));
    }

    @Test
    void doesNotFindTransactionForOtherUser() {
        User owner = persistUser("tx-owner2@example.com");
        User stranger = persistUser("tx-stranger@example.com");
        Category category = persistCategory(owner, TransactionType.INCOME);

        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("200.00"));
        transaction.setType(TransactionType.INCOME);
        transaction.setTransactionDate(LocalDate.now());
        transaction.setUser(owner);
        transaction.setCategory(category);
        transactionRepository.save(transaction);

        assertThat(transactionRepository.findByIdAndUser(transaction.getId(), stranger)).isEmpty();
    }
}
