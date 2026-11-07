package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.dto.CategoryResponse;
import com.ebookmarketplace.backend.dto.CreateCategoryRequest;
import com.ebookmarketplace.backend.entity.Category;
import com.ebookmarketplace.backend.exception.CategoryAlreadyExistsException;
import com.ebookmarketplace.backend.exception.ResourceNotFoundException;
import com.ebookmarketplace.backend.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {

        String name = request.name().trim();
        String slug = generateSlug(name);

        if (categoryRepository.existsByNameIgnoreCase(name)
                || categoryRepository.existsBySlug(slug)) {
            throw new CategoryAlreadyExistsException(
                    "A category with this name already exists"
            );
        }

        Category category = new Category(
                name,
                slug,
                request.description()
        );

        return toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse updateCategory(
            Long id,
            CreateCategoryRequest request
    ) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found")
                );

        String name = request.name().trim();
        String slug = generateSlug(name);

        categoryRepository.findBySlug(slug)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new CategoryAlreadyExistsException(
                            "A category with this name already exists"
                    );
                });

        category.update(name, slug, request.description());

        return toResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategoryStatus(Long id, boolean active) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found")
                );

        category.setActive(active);

        return toResponse(category);
    }

    private String generateSlug(String value) {
        String normalized = Normalizer.normalize(
                value,
                Normalizer.Form.NFD
        );

        return normalized
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.isActive()
        );
    }
}