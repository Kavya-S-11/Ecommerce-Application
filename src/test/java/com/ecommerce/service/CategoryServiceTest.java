package com.ecommerce.service;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Category;
import com.ecommerce.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category electronics;
    private Category clothing;

    @BeforeEach
    void setUp() {
        electronics = new Category("Electronics", "Electronic devices");
        electronics.setId(1L);

        clothing = new Category("Clothing", "Apparel items");
        clothing.setId(2L);
    }

    // --- getAllCategories ---

    @Test
    void getAllCategories_returnsAllCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(electronics, clothing));

        List<Category> result = categoryService.getAllCategories();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Category::getName).containsExactlyInAnyOrder("Electronics", "Clothing");
        verify(categoryRepository).findAll();
    }

    @Test
    void getAllCategories_emptyList_returnsEmpty() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<Category> result = categoryService.getAllCategories();

        assertThat(result).isEmpty();
    }

    // --- getCategoryById ---

    @Test
    void getCategoryById_existingId_returnsCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));

        Category result = categoryService.getCategoryById(1L);

        assertThat(result.getName()).isEqualTo("Electronics");
    }

    @Test
    void getCategoryById_nonExistingId_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- createCategory ---

    @Test
    void createCategory_newName_savesAndReturnsCategory() {
        when(categoryRepository.existsByNameIgnoreCase("Books")).thenReturn(false);
        Category books = new Category("Books", "Educational materials");
        when(categoryRepository.save(books)).thenReturn(books);

        Category result = categoryService.createCategory(books);

        assertThat(result.getName()).isEqualTo("Books");
        verify(categoryRepository).save(books);
    }

    @Test
    void createCategory_duplicateName_throwsBadRequestException() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(electronics))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Electronics");

        verify(categoryRepository, never()).save(any());
    }

    // --- updateCategory ---

    @Test
    void updateCategory_existingId_updatesAndReturns() {
        Category updated = new Category("Updated Electronics", "New description");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category result = categoryService.updateCategory(1L, updated);

        assertThat(result.getName()).isEqualTo("Updated Electronics");
        assertThat(result.getDescription()).isEqualTo("New description");
    }

    @Test
    void updateCategory_nonExistingId_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(99L, electronics))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).save(any());
    }

    // --- deleteCategory ---

    @Test
    void deleteCategory_existingId_deletesCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(electronics);
    }

    @Test
    void deleteCategory_nonExistingId_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).delete(any());
    }
}
