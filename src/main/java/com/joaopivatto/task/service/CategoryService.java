package com.joaopivatto.task.service;

import com.joaopivatto.task.entity.Category;
import com.joaopivatto.task.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        if (id == null) {
            return null;
        }
        return categoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Category not found for id: " + id));
    }

    @Transactional
    public void ensureDefaults(List<String> categoryNames) {
        for (String name : categoryNames) {
            categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName(name);
                    return categoryRepository.save(category);
                });
        }
    }
}

