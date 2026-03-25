package com.ecommerce.config;

import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        // Create categories
        Category electronics = categoryRepository.save(new Category("Electronics", "Electronic devices and gadgets"));
        Category clothing = categoryRepository.save(new Category("Clothing", "Apparel and fashion items"));
        Category books = categoryRepository.save(new Category("Books", "Books and educational materials"));

        // Create products
        productRepository.save(new Product("Laptop Pro 15", "High-performance laptop with 16GB RAM", new BigDecimal("1299.99"), 50, electronics));
        productRepository.save(new Product("Wireless Earbuds", "Noise-cancelling bluetooth earbuds", new BigDecimal("89.99"), 200, electronics));
        productRepository.save(new Product("Smartphone X12", "Latest flagship smartphone", new BigDecimal("799.99"), 75, electronics));
        productRepository.save(new Product("Classic T-Shirt", "100% cotton comfortable t-shirt", new BigDecimal("19.99"), 500, clothing));
        productRepository.save(new Product("Denim Jeans", "Slim fit denim jeans", new BigDecimal("49.99"), 300, clothing));
        productRepository.save(new Product("Spring Boot in Action", "Learn Spring Boot from scratch", new BigDecimal("39.99"), 100, books));
        productRepository.save(new Product("Clean Code", "A handbook of agile software craftsmanship", new BigDecimal("34.99"), 80, books));

        // Create sample users
        userRepository.save(new User("Alice Johnson", "alice@example.com", "password123", "123 Main St, New York"));
        userRepository.save(new User("Bob Smith", "bob@example.com", "password456", "456 Oak Ave, Los Angeles"));

        System.out.println("=== Sample data loaded successfully ===");
        System.out.println("Categories: " + categoryRepository.count());
        System.out.println("Products: " + productRepository.count());
        System.out.println("Users: " + userRepository.count());
    }
}
