package com.ecommerce.controller;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.User;
import com.ecommerce.service.UserService;
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

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = new User("Alice Johnson", "alice@example.com", "pass123", "123 Main St");
        alice.setId(1L);

        bob = new User("Bob Smith", "bob@example.com", "pass456", "456 Oak Ave");
        bob.setId(2L);
    }

    // --- GET /api/users ---

    @Test
    void getAllUsers_returnsListWithStatus200() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(alice, bob));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Alice Johnson")))
                .andExpect(jsonPath("$[1].name", is("Bob Smith")));
    }

    @Test
    void getAllUsers_passwordNotExposed() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(alice));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    // --- GET /api/users/{id} ---

    @Test
    void getUserById_existingId_returns200() throws Exception {
        when(userService.getUserById(1L)).thenReturn(alice);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Alice Johnson")))
                .andExpect(jsonPath("$.email", is("alice@example.com")))
                .andExpect(jsonPath("$.address", is("123 Main St")));
    }

    @Test
    void getUserById_nonExistingId_returns404() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("99")));
    }

    // --- POST /api/users/register ---

    @Test
    void registerUser_validBody_returns201() throws Exception {
        when(userService.registerUser(any(User.class))).thenReturn(alice);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alice)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Alice Johnson")));
    }

    @Test
    void registerUser_blankName_returns400() throws Exception {
        User invalid = new User("", "valid@email.com", "pass", "addr");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", notNullValue()));
    }

    @Test
    void registerUser_invalidEmail_returns400() throws Exception {
        User invalid = new User("Valid Name", "not-an-email", "pass", "addr");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email", notNullValue()));
    }

    @Test
    void registerUser_blankPassword_returns400() throws Exception {
        User invalid = new User("Valid Name", "valid@email.com", "", "addr");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    void registerUser_duplicateEmail_returns400() throws Exception {
        when(userService.registerUser(any(User.class)))
                .thenThrow(new BadRequestException("Email already registered: alice@example.com"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alice)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("alice@example.com")));
    }

    // --- PUT /api/users/{id} ---

    @Test
    void updateUser_existingId_returns200() throws Exception {
        User updated = new User("Alice Updated", "alice@example.com", "pass", "999 New St");
        updated.setId(1L);
        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(updated);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice Updated")))
                .andExpect(jsonPath("$.address", is("999 New St")));
    }

    @Test
    void updateUser_nonExistingId_returns404() throws Exception {
        when(userService.updateUser(eq(99L), any(User.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(put("/api/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alice)))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/users/{id} ---

    @Test
    void deleteUser_existingId_returns204() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    void deleteUser_nonExistingId_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 99"))
                .when(userService).deleteUser(99L);

        mockMvc.perform(delete("/api/users/99"))
                .andExpect(status().isNotFound());
    }
}
