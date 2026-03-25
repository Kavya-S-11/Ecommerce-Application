package com.ecommerce.controller;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

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

    // --- GET /api/products ---

    @Test
    void getAllProducts_noFilter_returnsAllProducts() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(laptop, phone));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Laptop Pro")))
                .andExpect(jsonPath("$[1].name", is("Smartphone X")));
    }

    @Test
    void getAllProducts_withSearchParam_callsSearch() throws Exception {
        when(productService.searchProducts("laptop")).thenReturn(List.of(laptop));

        mockMvc.perform(get("/api/products").param("search", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Laptop Pro")));

        verify(productService).searchProducts("laptop");
    }

    @Test
    void getAllProducts_withCategoryIdParam_callsGetByCategory() throws Exception {
        when(productService.getProductsByCategory(1L)).thenReturn(List.of(laptop, phone));

        mockMvc.perform(get("/api/products").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(productService).getProductsByCategory(1L);
    }

    @Test
    void getAllProducts_withAvailableParam_callsGetAvailable() throws Exception {
        when(productService.getAvailableProducts()).thenReturn(List.of(laptop));

        mockMvc.perform(get("/api/products").param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(productService).getAvailableProducts();
    }

    // --- GET /api/products/{id} ---

    @Test
    void getProductById_existingId_returns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(laptop);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Laptop Pro")))
                .andExpect(jsonPath("$.price", is(1299.99)))
                .andExpect(jsonPath("$.stock", is(10)));
    }

    @Test
    void getProductById_nonExistingId_returns404() throws Exception {
        when(productService.getProductById(99L)).thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("99")));
    }

    // --- POST /api/products ---

    @Test
    void createProduct_validBody_returns201() throws Exception {
        when(productService.createProduct(any(Product.class), isNull())).thenReturn(laptop);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptop)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Laptop Pro")));
    }

    @Test
    void createProduct_withCategoryId_callsServiceWithCategoryId() throws Exception {
        when(productService.createProduct(any(Product.class), eq(1L))).thenReturn(laptop);

        mockMvc.perform(post("/api/products")
                        .param("categoryId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptop)))
                .andExpect(status().isCreated());

        verify(productService).createProduct(any(Product.class), eq(1L));
    }

    @Test
    void createProduct_blankName_returns400() throws Exception {
        Product invalid = new Product("", "desc", new BigDecimal("10.00"), 5, null);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", notNullValue()));
    }

    @Test
    void createProduct_negativePriceField_returns400() throws Exception {
        Product invalid = new Product("Valid Name", "desc", new BigDecimal("-1.00"), 5, null);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price", notNullValue()));
    }

    // --- PUT /api/products/{id} ---

    @Test
    void updateProduct_existingId_returns200() throws Exception {
        Product updated = new Product("Laptop Ultra", "Updated", new BigDecimal("1499.99"), 8, electronics);
        updated.setId(1L);
        when(productService.updateProduct(eq(1L), any(Product.class), isNull())).thenReturn(updated);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Laptop Ultra")));
    }

    @Test
    void updateProduct_nonExistingId_returns404() throws Exception {
        when(productService.updateProduct(eq(99L), any(Product.class), isNull()))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(put("/api/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptop)))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/products/{id} ---

    @Test
    void deleteProduct_existingId_returns204() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }

    @Test
    void deleteProduct_nonExistingId_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found with id: 99"))
                .when(productService).deleteProduct(99L);

        mockMvc.perform(delete("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /api/products/{id}/stock ---

    @Test
    void updateStock_validQuantity_returns200() throws Exception {
        laptop.setStock(15);
        when(productService.updateStock(1L, 5)).thenReturn(laptop);

        mockMvc.perform(patch("/api/products/1/stock").param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(15)));
    }

    @Test
    void updateStock_nonExistingId_returns404() throws Exception {
        when(productService.updateStock(99L, 5)).thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(patch("/api/products/99/stock").param("quantity", "5"))
                .andExpect(status().isNotFound());
    }
}
