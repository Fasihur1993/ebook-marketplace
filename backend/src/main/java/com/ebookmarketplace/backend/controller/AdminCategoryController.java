package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.dto.ApiResponse;
import com.ebookmarketplace.backend.dto.CategoryResponse;
import com.ebookmarketplace.backend.dto.CreateCategoryRequest;
import com.ebookmarketplace.backend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        CategoryResponse response =
                categoryService.createCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Category created successfully",
                        response
                ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Categories retrieved successfully",
                        categoryService.getAllCategories()
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Category updated successfully",
                        categoryService.updateCategory(id, request)
                )
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategoryStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Category status updated successfully",
                        categoryService.updateCategoryStatus(id, active)
                )
        );
    }
}