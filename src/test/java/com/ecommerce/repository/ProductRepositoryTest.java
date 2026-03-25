package com.ecommerce.repository;

import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;
    private Category clothing;
    private Product laptop;
    private Product phone;
    private Product tshirt;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        electronics = categoryRepository.save(new Category("Electronics", "Devices"));
        clothing = categoryRepository.save(new Category("Clothing", "Apparel"));

        laptop = productRepository.save(new Product("Laptop Pro", "High-end laptop", new BigDecimal("1299.99"), 10, electronics));
        phone = productRepository.save(new Product("Smartphone X", "Latest phone", new BigDecimal("799.99"), 0, electronics));
        tshirt = productRepository.save(new Product("Classic T-Shirt", "Cotton shirt", new BigDecimal("19.99"), 100, clothing));
    }

    @Test
    void findAll_returnsAllProducts() {
        List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(3);
    }

    @Test
    void findById_existingId_returnsProduct() {
        Optional<Product> result = productRepository.findById(laptop.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Laptop Pro");
    }

    @Test
    void findById_nonExistingId_returnsEmpty() {
        Optional<Product> result = productRepository.findById(999L);
        assertThat(result).isEmpty();
    }

    @Test
    void findByCategoryId_returnsProductsInCategory() {
        List<Product> result = productRepository.findByCategoryId(electronics.getId());
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getName).containsExactlyInAnyOrder("Laptop Pro", "Smartphone X");
    }

    @Test
    void findByCategoryId_categoryWithNoProducts_returnsEmpty() {
        Category empty = categoryRepository.save(new Category("Books", "Educational"));
        List<Product> result = productRepository.findByCategoryId(empty.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void findByNameContainingIgnoreCase_partialMatch_returnsProducts() {
        List<Product> result = productRepository.findByNameContainingIgnoreCase("laptop");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Laptop Pro");
    }

    @Test
    void findByNameContainingIgnoreCase_matchesMultiple() {
        List<Product> result = productRepository.findByNameContainingIgnoreCase("a");
        assertThat(result).hasSizeGreaterThan(1);
    }

    @Test
    void findByNameContainingIgnoreCase_noMatch_returnsEmpty() {
        List<Product> result = productRepository.findByNameContainingIgnoreCase("zzzzzz");
        assertThat(result).isEmpty();
    }

    @Test
    void findByStockGreaterThan_zero_returnsInStockProducts() {
        List<Product> result = productRepository.findByStockGreaterThan(0);
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getName).containsExactlyInAnyOrder("Laptop Pro", "Classic T-Shirt");
    }

    @Test
    void findByStockGreaterThan_highThreshold_returnsHighStockProducts() {
        List<Product> result = productRepository.findByStockGreaterThan(50);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Classic T-Shirt");
    }

    @Test
    void save_persistsProduct() {
        Product book = new Product("Clean Code", "Programming book", new BigDecimal("34.99"), 50, null);
        Product saved = productRepository.save(book);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPrice()).isEqualByComparingTo("34.99");
    }

    @Test
    void delete_removesProduct() {
        productRepository.delete(phone);
        Optional<Product> result = productRepository.findById(phone.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void update_changesStock() {
        laptop.setStock(5);
        productRepository.save(laptop);

        Optional<Product> updated = productRepository.findById(laptop.getId());
        assertThat(updated.get().getStock()).isEqualTo(5);
    }
}
