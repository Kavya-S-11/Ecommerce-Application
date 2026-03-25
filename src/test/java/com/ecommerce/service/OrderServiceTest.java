package com.ecommerce.service;

import com.ecommerce.dto.OrderRequest;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.*;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserService userService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private User alice;
    private Product laptop;
    private Product phone;
    private Order pendingOrder;
    private Order deliveredOrder;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "pass", "123 Main St");
        alice.setId(1L);

        laptop = new Product("Laptop Pro", "High-end", new BigDecimal("1299.99"), 10, null);
        laptop.setId(1L);

        phone = new Product("Smartphone X", "Latest", new BigDecimal("799.99"), 5, null);
        phone.setId(2L);

        pendingOrder = new Order(alice, "123 Main St");
        pendingOrder.setId(1L);
        pendingOrder.setStatus(Order.Status.PENDING);

        deliveredOrder = new Order(alice, "123 Main St");
        deliveredOrder.setId(2L);
        deliveredOrder.setStatus(Order.Status.DELIVERED);
    }

    // --- getAllOrders ---

    @Test
    void getAllOrders_returnsAllOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(pendingOrder, deliveredOrder));

        List<Order> result = orderService.getAllOrders();

        assertThat(result).hasSize(2);
    }

    // --- getOrderById ---

    @Test
    void getOrderById_existingId_returnsOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        Order result = orderService.getOrderById(1L);

        assertThat(result.getStatus()).isEqualTo(Order.Status.PENDING);
    }

    @Test
    void getOrderById_nonExistingId_throwsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- getOrdersByUser ---

    @Test
    void getOrdersByUser_validUser_returnsOrders() {
        when(userService.getUserById(1L)).thenReturn(alice);
        when(orderRepository.findByUserId(1L)).thenReturn(List.of(pendingOrder));

        List<Order> result = orderService.getOrdersByUser(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getOrdersByUser_nonExistingUser_throwsResourceNotFoundException() {
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        assertThatThrownBy(() -> orderService.getOrdersByUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- placeOrder ---

    @Test
    void placeOrder_validRequest_createsOrderAndDeductsStock() {
        OrderRequest request = buildOrderRequest(1L, 1L, 2);

        when(userService.getUserById(1L)).thenReturn(alice);
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));
        when(productRepository.save(any(Product.class))).thenReturn(laptop);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.placeOrder(request);

        assertThat(result.getUser()).isEqualTo(alice);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("2599.98"); // 1299.99 * 2
        assertThat(laptop.getStock()).isEqualTo(8); // 10 - 2
        verify(productRepository).save(laptop);
    }

    @Test
    void placeOrder_multipleItems_calculatesCorrectTotal() {
        OrderRequest request = new OrderRequest();
        request.setUserId(1L);
        request.setShippingAddress("123 Main St");

        OrderRequest.OrderItemRequest item1 = new OrderRequest.OrderItemRequest();
        item1.setProductId(1L);
        item1.setQuantity(1);

        OrderRequest.OrderItemRequest item2 = new OrderRequest.OrderItemRequest();
        item2.setProductId(2L);
        item2.setQuantity(2);

        request.setItems(List.of(item1, item2));

        when(userService.getUserById(1L)).thenReturn(alice);
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));
        when(productRepository.findById(2L)).thenReturn(Optional.of(phone));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.placeOrder(request);

        // 1299.99 * 1 + 799.99 * 2 = 2899.97
        assertThat(result.getTotalAmount()).isEqualByComparingTo("2899.97");
        assertThat(result.getItems()).hasSize(2);
    }

    @Test
    void placeOrder_insufficientStock_throwsBadRequestException() {
        OrderRequest request = buildOrderRequest(1L, 1L, 100); // request 100, only 10 available

        when(userService.getUserById(1L)).thenReturn(alice);
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_nonExistingProduct_throwsResourceNotFoundException() {
        OrderRequest request = buildOrderRequest(1L, 99L, 1);

        when(userService.getUserById(1L)).thenReturn(alice);
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void placeOrder_nonExistingUser_throwsResourceNotFoundException() {
        OrderRequest request = buildOrderRequest(99L, 1L, 1);
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- updateOrderStatus ---

    @Test
    void updateOrderStatus_validStatus_updatesOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.updateOrderStatus(1L, "CONFIRMED");

        assertThat(result.getStatus()).isEqualTo(Order.Status.CONFIRMED);
    }

    @Test
    void updateOrderStatus_caseInsensitive_updatesOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.updateOrderStatus(1L, "shipped");

        assertThat(result.getStatus()).isEqualTo(Order.Status.SHIPPED);
    }

    @Test
    void updateOrderStatus_invalidStatus_throwsBadRequestException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "INVALID_STATUS"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status");
    }

    @Test
    void updateOrderStatus_nonExistingOrder_throwsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(99L, "CONFIRMED"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- cancelOrder ---

    @Test
    void cancelOrder_pendingOrder_cancelsAndRestoresStock() {
        OrderItem item = new OrderItem(pendingOrder, laptop, 2);
        pendingOrder.setItems(new ArrayList<>(List.of(item)));
        laptop.setStock(8); // already deducted

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(productRepository.save(any(Product.class))).thenReturn(laptop);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.cancelOrder(1L);

        assertThat(result.getStatus()).isEqualTo(Order.Status.CANCELLED);
        assertThat(laptop.getStock()).isEqualTo(10); // 8 + 2 restored
    }

    @Test
    void cancelOrder_deliveredOrder_throwsBadRequestException() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(deliveredOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(2L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot cancel a delivered order");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_nonExistingOrder_throwsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class);
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
