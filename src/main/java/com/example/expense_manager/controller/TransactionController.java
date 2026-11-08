package com.example.expense_manager.controller;

import com.example.expense_manager.dto.TransactionCreateRequest;
import com.example.expense_manager.dto.TransactionResponse;
import com.example.expense_manager.dto.TransactionUpdateRequest;
import com.example.expense_manager.entity.TransactionType;
import com.example.expense_manager.security.UserPrincipal;
import com.example.expense_manager.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public Page<TransactionResponse> list(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String month,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        return transactionService.list(principal.getUser(), type, categoryId, month, pageable);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return transactionService.get(id, principal.getUser());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionCreateRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return transactionService.create(request, principal.getUser());
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable Long id,
                                       @Valid @RequestBody TransactionUpdateRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return transactionService.update(id, request, principal.getUser());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        transactionService.delete(id, principal.getUser());
    }
}
