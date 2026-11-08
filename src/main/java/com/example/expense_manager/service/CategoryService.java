package com.example.expense_manager.service;

import com.example.expense_manager.dto.CategoryCreateRequest;
import com.example.expense_manager.dto.CategoryResponse;
import com.example.expense_manager.dto.CategoryUpdateRequest;
import com.example.expense_manager.entity.Category;
import com.example.expense_manager.entity.TransactionType;
import com.example.expense_manager.entity.User;
import com.example.expense_manager.repository.CategoryRepository;
import com.example.expense_manager.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<CategoryResponse> list(User owner) {
        return categoryRepository.findByUser(owner).stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse get(Long id, User owner) {
        return toResponse(findOwned(id, owner));
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request, User owner) {
        String name = request.name().trim();
        rejectIfDuplicate(owner, request.type(), name);

        Category category = new Category();
        category.setName(name);
        category.setType(request.type());
        category.setUser(owner);

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryUpdateRequest request, User owner) {
        Category category = findOwned(id, owner);
        String name = request.name().trim();

        boolean typeChanged = request.type() != category.getType();
        boolean nameChanged = !name.equals(category.getName());

        if (typeChanged && transactionRepository.existsByCategoryAndTypeNot(category, request.type())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change category type: existing transactions use a different type");
        }

        if (typeChanged || nameChanged) {
            rejectIfDuplicate(owner, request.type(), name);
        }

        category.setName(name);
        category.setType(request.type());

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id, User owner) {
        Category category = findOwned(id, owner);
        if (transactionRepository.existsByCategory(category)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category is used by existing transactions");
        }
        categoryRepository.delete(category);
    }

    private void rejectIfDuplicate(User owner, TransactionType type, String name) {
        if (categoryRepository.existsByUserAndTypeAndName(owner, type, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category with this name and type already exists");
        }
    }

    private Category findOwned(Long id, User owner) {
        return categoryRepository.findByIdAndUser(id, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getType());
    }
}
