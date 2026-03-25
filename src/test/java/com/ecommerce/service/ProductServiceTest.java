package com.ecommerce.service;

import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductService productService;

    private Category electronics;
    private Product laptop;
    private Product phone;

    @BeforeEach
    void setUp() {
        electronics = new Category("Electronics", "Devices");
        electronics.setId(1L);

        laptop = new Product("Laptop Pro", "High-end laptop", new BigDecimal("1299.99"), 10, electronics);
        laptop.setId(1L);

        phone = new Product("Smartphone X", "Latest phone", new BigDecimal("799.99"), 5, electronics);
        phone.setId(2L);
    }

    // --- getAllProducts ---

    @Test
    void getAllProducts_returnsAll() {
        when(productRepository.findAll()).thenReturn(List.of(laptop, phone));

        List<Product> result = productService.getAllProducts();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllProducts_emptyRepo_returnsEmpty() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<Product> result = productService.getAllProducts();

        assertThat(result).isEmpty();
    }

    // --- getProductById ---

    @Test
    void getProductById_existingId_returnsProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));

        Product result = productService.getProductById(1L);

        assertThat(result.getName()).isEqualTo("Laptop Pro");
    }

    @Test
    void getProductById_nonExistingId_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- getProductsByCategory ---

    @Test
    void getProductsByCategory_validCategory_returnsProducts() {
        when(productRepository.findByCategoryId(1L)).thenReturn(List.of(laptop, phone));

        List<Product> result = productService.getProductsByCategory(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    void getProductsByCategory_noProducts_returnsEmpty() {
        when(productRepository.findByCategoryId(99L)).thenReturn(List.of());

        List<Product> result = productService.getProductsByCategory(99L);

        assertThat(result).isEmpty();
    }

    // --- searchProducts ---

    @Test
    void searchProducts_matchingName_returnsResults() {
        when(productRepository.findByNameContainingIgnoreCase("laptop")).thenReturn(List.of(laptop));

        List<Product> result = productService.searchProducts("laptop");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Laptop Pro");
    }

    @Test
    void searchProducts_noMatch_returnsEmpty() {
        when(productRepository.findByNameContainingIgnoreCase("xyz")).thenReturn(List.of());

        List<Product> result = productService.searchProducts("xyz");

        assertThat(result).isEmpty();
    }

    // --- getAvailableProducts ---

    @Test
    void getAvailableProducts_returnsInStockProducts() {
        when(productRepository.findByStockGreaterThan(0)).thenReturn(List.of(laptop, phone));

        List<Product> result = productService.getAvailableProducts();

        assertThat(result).hasSize(2);
    }

    // --- createProduct ---

    @Test
    void createProduct_withCategory_setsAndSaves() {
        when(categoryService.getCategoryById(1L)).thenReturn(electronics);
        when(productRepository.save(any(Product.class))).thenReturn(laptop);

        Product result = productService.createProduct(laptop, 1L);

        assertThat(result.getName()).isEqualTo("Laptop Pro");
        assertThat(result.getCategory()).isEqualTo(electronics);
        verify(productRepository).save(laptop);
    }

    @Test
    void createProduct_withoutCategory_savesWithoutCategory() {
        Product noCategory = new Product("Generic Item", "No cat", new BigDecimal("9.99"), 20, null);
        when(productRepository.save(noCategory)).thenReturn(noCategory);

        Product result = productService.createProduct(noCategory, null);

        assertThat(result.getCategory()).isNull();
        verify(categoryService, never()).getCategoryById(any());
    }

    @Test
    void createProduct_invalidCategoryId_throwsResourceNotFoundException() {
        when(categoryService.getCategoryById(99L)).thenThrow(new ResourceNotFoundException("Category not found with id: 99"));

        assertThatThrownBy(() -> productService.createProduct(laptop, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- updateProduct ---

    @Test
    void updateProduct_existingId_updatesFields() {
        Product updated = new Product("Laptop Ultra", "Even better", new BigDecimal("1499.99"), 15, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.updateProduct(1L, updated, null);

        assertThat(result.getName()).isEqualTo("Laptop Ultra");
        assertThat(result.getPrice()).isEqualByComparingTo("1499.99");
        assertThat(result.getStock()).isEqualTo(15);
    }

    @Test
    void updateProduct_withNewCategory_updatesCategory() {
        Category newCat = new Category("Computers", "PC stuff");
        newCat.setId(2L);
        Product updated = new Product("Laptop Pro", "Same laptop", new BigDecimal("1299.99"), 10, null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));
        when(categoryService.getCategoryById(2L)).thenReturn(newCat);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.updateProduct(1L, updated, 2L);

        assertThat(result.getCategory()).isEqualTo(newCat);
    }

    @Test
    void updateProduct_nonExistingId_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, laptop, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- deleteProduct ---

    @Test
    void deleteProduct_existingId_deletesProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));

        productService.deleteProduct(1L);

        verify(productRepository).delete(laptop);
    }

    @Test
    void deleteProduct_nonExistingId_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).delete(any());
    }

    // --- updateStock ---

    @Test
    void updateStock_addsQuantity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.updateStock(1L, 5);

        assertThat(result.getStock()).isEqualTo(15); // 10 + 5
    }

    @Test
    void updateStock_negativeQuantity_reducesStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.updateStock(1L, -3);

        assertThat(result.getStock()).isEqualTo(7); // 10 - 3
    }

    @Test
    void updateStock_nonExistingId_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateStock(99L, 5))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
