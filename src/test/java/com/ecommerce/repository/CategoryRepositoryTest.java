package com.ecommerce.repository;

import com.ecommerce.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;
    private Category clothing;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
        electronics = categoryRepository.save(new Category("Electronics", "Electronic devices"));
        clothing = categoryRepository.save(new Category("Clothing", "Apparel items"));
    }

    @Test
    void findAll_returnsAllCategories() {
        List<Category> result = categoryRepository.findAll();
        assertThat(result).hasSize(2);
    }

    @Test
    void findById_existingId_returnsCategory() {
        Optional<Category> result = categoryRepository.findById(electronics.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Electronics");
    }

    @Test
    void findById_nonExistingId_returnsEmpty() {
        Optional<Category> result = categoryRepository.findById(999L);
        assertThat(result).isEmpty();
    }

    @Test
    void findByNameIgnoreCase_exactMatch_returnsCategory() {
        Optional<Category> result = categoryRepository.findByNameIgnoreCase("Electronics");
        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).isEqualTo("Electronic devices");
    }

    @Test
    void findByNameIgnoreCase_upperCase_returnsCategory() {
        Optional<Category> result = categoryRepository.findByNameIgnoreCase("ELECTRONICS");
        assertThat(result).isPresent();
    }

    @Test
    void findByNameIgnoreCase_lowerCase_returnsCategory() {
        Optional<Category> result = categoryRepository.findByNameIgnoreCase("clothing");
        assertThat(result).isPresent();
    }

    @Test
    void findByNameIgnoreCase_notFound_returnsEmpty() {
        Optional<Category> result = categoryRepository.findByNameIgnoreCase("Books");
        assertThat(result).isEmpty();
    }

    @Test
    void existsByNameIgnoreCase_existingName_returnsTrue() {
        boolean exists = categoryRepository.existsByNameIgnoreCase("Electronics");
        assertThat(exists).isTrue();
    }

    @Test
    void existsByNameIgnoreCase_mixedCase_returnsTrue() {
        boolean exists = categoryRepository.existsByNameIgnoreCase("eLECTRONICS");
        assertThat(exists).isTrue();
    }

    @Test
    void existsByNameIgnoreCase_nonExistingName_returnsFalse() {
        boolean exists = categoryRepository.existsByNameIgnoreCase("Sports");
        assertThat(exists).isFalse();
    }

    @Test
    void save_persistsCategory() {
        Category books = new Category("Books", "Educational materials");
        Category saved = categoryRepository.save(books);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Books");
    }

    @Test
    void delete_removesCategory() {
        categoryRepository.delete(electronics);
        Optional<Category> result = categoryRepository.findById(electronics.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void update_changesName() {
        electronics.setName("Consumer Electronics");
        categoryRepository.save(electronics);

        Optional<Category> updated = categoryRepository.findById(electronics.getId());
        assertThat(updated.get().getName()).isEqualTo("Consumer Electronics");
    }
}
