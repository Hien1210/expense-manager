package com.example.expense_manager.repository;

import com.example.expense_manager.entity.Category;
import com.example.expense_manager.entity.TransactionType;
import com.example.expense_manager.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void savesCategoryBelongingToUser() {
        User owner = new User();
        owner.setEmail("owner@example.com");
        owner.setPasswordHash("hash");
        userRepository.save(owner);

        Category category = new Category();
        category.setName("Salary");
        category.setType(TransactionType.INCOME);
        category.setUser(owner);

        Category saved = categoryRepository.save(category);

        assertThat(saved.getId()).isNotNull();
        assertThat(categoryRepository.findByIdAndUser(saved.getId(), owner)).isPresent();
    }

    @Test
    void doesNotFindCategoryForOtherUser() {
        User owner = new User();
        owner.setEmail("owner2@example.com");
        owner.setPasswordHash("hash");
        userRepository.save(owner);

        User stranger = new User();
        stranger.setEmail("stranger@example.com");
        stranger.setPasswordHash("hash");
        userRepository.save(stranger);

        Category category = new Category();
        category.setName("Rent");
        category.setType(TransactionType.EXPENSE);
        category.setUser(owner);
        categoryRepository.save(category);

        assertThat(categoryRepository.findByIdAndUser(category.getId(), stranger)).isEmpty();
    }
}
