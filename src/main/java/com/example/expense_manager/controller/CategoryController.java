package com.example.expense_manager.controller;

import com.example.expense_manager.dto.CategoryCreateRequest;
import com.example.expense_manager.dto.CategoryResponse;
import com.example.expense_manager.dto.CategoryUpdateRequest;
import com.example.expense_manager.security.UserPrincipal;
import com.example.expense_manager.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return categoryService.list(principal.getUser());
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return categoryService.get(id, principal.getUser());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryCreateRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return categoryService.create(request, principal.getUser());
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id,
                                    @Valid @RequestBody CategoryUpdateRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return categoryService.update(id, request, principal.getUser());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        categoryService.delete(id, principal.getUser());
    }
}
