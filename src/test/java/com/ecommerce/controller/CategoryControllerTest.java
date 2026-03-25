package com.ecommerce.controller;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Category;
import com.ecommerce.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private Category electronics;
    private Category clothing;

    @BeforeEach
    void setUp() {
        electronics = new Category("Electronics", "Electronic devices");
        electronics.setId(1L);

        clothing = new Category("Clothing", "Apparel items");
        clothing.setId(2L);
    }

    // --- GET /api/categories ---

    @Test
    void getAllCategories_returnsListWithStatus200() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(electronics, clothing));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Electronics")))
                .andExpect(jsonPath("$[1].name", is("Clothing")));
    }

    @Test
    void getAllCategories_emptyList_returnsEmptyArray() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- GET /api/categories/{id} ---

    @Test
    void getCategoryById_existingId_returnsCategory() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(electronics);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Electronics")))
                .andExpect(jsonPath("$.description", is("Electronic devices")));
    }

    @Test
    void getCategoryById_nonExistingId_returns404() throws Exception {
        when(categoryService.getCategoryById(99L)).thenThrow(new ResourceNotFoundException("Category not found with id: 99"));

        mockMvc.perform(get("/api/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("99")));
    }

    // --- POST /api/categories ---

    @Test
    void createCategory_validBody_returns201() throws Exception {
        Category input = new Category("Books", "Educational materials");
        Category saved = new Category("Books", "Educational materials");
        saved.setId(3L);

        when(categoryService.createCategory(any(Category.class))).thenReturn(saved);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("Books")));
    }

    @Test
    void createCategory_blankName_returns400() throws Exception {
        Category invalid = new Category("", "desc");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", notNullValue()));
    }

    @Test
    void createCategory_duplicateName_returns400() throws Exception {
        when(categoryService.createCategory(any(Category.class)))
                .thenThrow(new BadRequestException("Category already exists with name: Electronics"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(electronics)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Electronics")));
    }

    // --- PUT /api/categories/{id} ---

    @Test
    void updateCategory_existingId_returns200() throws Exception {
        Category updated = new Category("Updated Electronics", "New desc");
        updated.setId(1L);

        when(categoryService.updateCategory(eq(1L), any(Category.class))).thenReturn(updated);

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Electronics")));
    }

    @Test
    void updateCategory_nonExistingId_returns404() throws Exception {
        when(categoryService.updateCategory(eq(99L), any(Category.class)))
                .thenThrow(new ResourceNotFoundException("Category not found with id: 99"));

        mockMvc.perform(put("/api/categories/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(electronics)))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/categories/{id} ---

    @Test
    void deleteCategory_existingId_returns204() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(1L);
    }

    @Test
    void deleteCategory_nonExistingId_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Category not found with id: 99"))
                .when(categoryService).deleteCategory(99L);

        mockMvc.perform(delete("/api/categories/99"))
                .andExpect(status().isNotFound());
    }
}
