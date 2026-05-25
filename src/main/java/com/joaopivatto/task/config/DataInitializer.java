package com.joaopivatto.task.config;

import com.joaopivatto.task.service.CategoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedCategories(CategoryService categoryService) {
        return args -> categoryService.ensureDefaults(List.of("College", "Work", "Personal", "Finance"));
    }
}

