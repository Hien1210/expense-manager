package com.example.expense_manager.repository;

import com.example.expense_manager.dto.CategoryTotalResponse;
import com.example.expense_manager.entity.Category;
import com.example.expense_manager.entity.Transaction;
import com.example.expense_manager.entity.TransactionType;
import com.example.expense_manager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUser(User user);

    Optional<Transaction> findByIdAndUser(Long id, User user);

    boolean existsByCategory(Category category);

    boolean existsByCategoryAndTypeNot(Category category, TransactionType type);

    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.category
            WHERE t.user = :user
              AND (:type IS NULL OR t.type = :type)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
              AND (:startDate IS NULL OR t.transactionDate >= :startDate)
              AND (:endDate IS NULL OR t.transactionDate < :endDate)
            """)
    Page<Transaction> search(@Param("user") User user,
                              @Param("type") TransactionType type,
                              @Param("categoryId") Long categoryId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.user = :user AND t.type = :type
              AND t.transactionDate >= :startDate AND t.transactionDate < :endDate
            """)
    BigDecimal sumAmountByUserAndTypeAndDateRange(@Param("user") User user,
                                                   @Param("type") TransactionType type,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT new com.example.expense_manager.dto.CategoryTotalResponse(c.id, c.name, SUM(t.amount))
            FROM Transaction t JOIN t.category c
            WHERE t.user = :user AND t.type = :type
              AND t.transactionDate >= :startDate AND t.transactionDate < :endDate
            GROUP BY c.id, c.name
            ORDER BY SUM(t.amount) DESC
            """)
    List<CategoryTotalResponse> sumAmountByCategoryForUserAndTypeAndDateRange(@Param("user") User user,
                                                                               @Param("type") TransactionType type,
                                                                               @Param("startDate") LocalDate startDate,
                                                                               @Param("endDate") LocalDate endDate);
}
