package com.example.expense_manager.repository;

import com.example.expense_manager.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndReadsUser() {
        User user = new User();
        user.setEmail("test.user@example.com");
        user.setPasswordHash("hashed-password");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findByEmail("test.user@example.com")).isPresent();
    }

    @Test
    void rejectsDuplicateEmail() {
        User first = new User();
        first.setEmail("duplicate@example.com");
        first.setPasswordHash("hash1");
        userRepository.saveAndFlush(first);

        User second = new User();
        second.setEmail("duplicate@example.com");
        second.setPasswordHash("hash2");

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(second));
    }
}
