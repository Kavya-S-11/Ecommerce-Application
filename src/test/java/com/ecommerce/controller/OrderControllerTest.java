package com.ecommerce.controller;

import com.ecommerce.dto.OrderRequest;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Order;
import com.ecommerce.model.User;
import com.ecommerce.service.OrderService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private User alice;
    private Order pendingOrder;
    private Order confirmedOrder;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "pass", "123 Main St");
        alice.setId(1L);

        pendingOrder = new Order(alice, "123 Main St");
        pendingOrder.setId(1L);
        pendingOrder.setStatus(Order.Status.PENDING);
        pendingOrder.setTotalAmount(new BigDecimal("1299.99"));

        confirmedOrder = new Order(alice, "123 Main St");
        confirmedOrder.setId(2L);
        confirmedOrder.setStatus(Order.Status.CONFIRMED);
        confirmedOrder.setTotalAmount(new BigDecimal("799.99"));
    }

    // --- GET /api/orders ---

    @Test
    void getAllOrders_returnsListWithStatus200() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(pendingOrder, confirmedOrder));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")))
                .andExpect(jsonPath("$[1].status", is("CONFIRMED")));
    }

    @Test
    void getAllOrders_emptyList_returns200WithEmptyArray() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of());

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- GET /api/orders/{id} ---

    @Test
    void getOrderById_existingId_returns200() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(pendingOrder);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.totalAmount", is(1299.99)));
    }

    @Test
    void getOrderById_nonExistingId_returns404() throws Exception {
        when(orderService.getOrderById(99L)).thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("99")));
    }

    // --- GET /api/orders/user/{userId} ---

    @Test
    void getOrdersByUser_validUserId_returnsList() throws Exception {
        when(orderService.getOrdersByUser(1L)).thenReturn(List.of(pendingOrder, confirmedOrder));

        mockMvc.perform(get("/api/orders/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getOrdersByUser_nonExistingUser_returns404() throws Exception {
        when(orderService.getOrdersByUser(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/orders/user/99"))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/orders ---

    @Test
    void placeOrder_validRequest_returns201() throws Exception {
        OrderRequest request = buildOrderRequest(1L, 1L, 2);
        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(pendingOrder);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void placeOrder_nullUserId_returns400() throws Exception {
        OrderRequest request = new OrderRequest();
        request.setUserId(null);
        OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(1);
        request.setItems(List.of(item));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.userId", notNullValue()));
    }

    @Test
    void placeOrder_emptyItems_returns400() throws Exception {
        OrderRequest request = new OrderRequest();
        request.setUserId(1L);
        request.setItems(List.of());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.items", notNullValue()));
    }

    @Test
    void placeOrder_insufficientStock_returns400() throws Exception {
        OrderRequest request = buildOrderRequest(1L, 1L, 1000);
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new BadRequestException("Insufficient stock for product: Laptop Pro"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Insufficient stock")));
    }

    @Test
    void placeOrder_nonExistingUser_returns404() throws Exception {
        OrderRequest request = buildOrderRequest(99L, 1L, 1);
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /api/orders/{id}/status ---

    @Test
    void updateOrderStatus_validStatus_returns200() throws Exception {
        confirmedOrder.setStatus(Order.Status.SHIPPED);
        when(orderService.updateOrderStatus(1L, "SHIPPED")).thenReturn(confirmedOrder);

        mockMvc.perform(patch("/api/orders/1/status").param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SHIPPED")));
    }

    @Test
    void updateOrderStatus_invalidStatus_returns400() throws Exception {
        when(orderService.updateOrderStatus(1L, "INVALID"))
                .thenThrow(new BadRequestException("Invalid status: INVALID"));

        mockMvc.perform(patch("/api/orders/1/status").param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid status")));
    }

    @Test
    void updateOrderStatus_nonExistingOrder_returns404() throws Exception {
        when(orderService.updateOrderStatus(99L, "CONFIRMED"))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

        mockMvc.perform(patch("/api/orders/99/status").param("status", "CONFIRMED"))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /api/orders/{id}/cancel ---

    @Test
    void cancelOrder_pendingOrder_returns200WithCancelledStatus() throws Exception {
        pendingOrder.setStatus(Order.Status.CANCELLED);
        when(orderService.cancelOrder(1L)).thenReturn(pendingOrder);

        mockMvc.perform(patch("/api/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void cancelOrder_deliveredOrder_returns400() throws Exception {
        when(orderService.cancelOrder(2L))
                .thenThrow(new BadRequestException("Cannot cancel a delivered order"));

        mockMvc.perform(patch("/api/orders/2/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot cancel")));
    }

    @Test
    void cancelOrder_nonExistingOrder_returns404() throws Exception {
        when(orderService.cancelOrder(99L))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

        mockMvc.perform(patch("/api/orders/99/cancel"))
                .andExpect(status().isNotFound());
    }

    // --- Helper ---

    private OrderRequest buildOrderRequest(Long userId, Long productId, int quantity) {
        OrderRequest request = new OrderRequest();
        request.setUserId(userId);
        request.setShippingAddress("123 Main St");

        OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);

        request.setItems(List.of(item));
        return request;
    }
}
