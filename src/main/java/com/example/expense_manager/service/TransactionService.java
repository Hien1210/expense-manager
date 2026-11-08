package com.example.expense_manager.service;

import com.example.expense_manager.dto.TransactionCreateRequest;
import com.example.expense_manager.dto.TransactionResponse;
import com.example.expense_manager.dto.TransactionUpdateRequest;
import com.example.expense_manager.entity.Category;
import com.example.expense_manager.entity.Transaction;
import com.example.expense_manager.entity.TransactionType;
import com.example.expense_manager.entity.User;
import com.example.expense_manager.repository.CategoryRepository;
import com.example.expense_manager.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Set;

@Service
public class TransactionService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("transactionDate", "amount", "type", "id");
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<TransactionResponse> list(User owner, TransactionType type, Long categoryId, String month, Pageable pageable) {
        LocalDate startDate = null;
        LocalDate endDate = null;

        if (month != null && !month.isBlank()) {
            YearMonth yearMonth = parseMonth(month);
            startDate = yearMonth.atDay(1);
            endDate = yearMonth.plusMonths(1).atDay(1);
        }

        Pageable normalized = normalizePageable(pageable);

        return transactionRepository.search(owner, type, categoryId, startDate, endDate, normalized)
                .map(this::toResponse);
    }

    public TransactionResponse get(Long id, User owner) {
        return toResponse(findOwned(id, owner));
    }

    @Transactional
    public TransactionResponse create(TransactionCreateRequest request, User owner) {
        Category category = resolveCategory(request.categoryId(), request.type(), owner);

        Transaction transaction = new Transaction();
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setUser(owner);
        transaction.setCategory(category);

        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionUpdateRequest request, User owner) {
        Transaction transaction = findOwned(id, owner);
        Category category = resolveCategory(request.categoryId(), request.type(), owner);

        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setCategory(category);

        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(Long id, User owner) {
        Transaction transaction = findOwned(id, owner);
        transactionRepository.delete(transaction);
    }

    private Category resolveCategory(Long categoryId, TransactionType type, User owner) {
        Category category = categoryRepository.findByIdAndUser(categoryId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (category.getType() != type) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Category type does not match transaction type");
        }

        return category;
    }

    private Transaction findOwned(Long id, User owner) {
        return transactionRepository.findByIdAndUser(id, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid month format, expected yyyy-MM");
        }
    }

    private Pageable normalizePageable(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field: " + order.getProperty());
            }
        }

        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "transactionDate");

        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }

        int size = pageable.isPaged()
                ? Math.min(pageable.getPageSize(), MAX_PAGE_SIZE)
                : DEFAULT_PAGE_SIZE;
        int page = pageable.isPaged() ? pageable.getPageNumber() : 0;

        return PageRequest.of(page, size, sort);
    }

    private TransactionResponse toResponse(Transaction transaction) {
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
