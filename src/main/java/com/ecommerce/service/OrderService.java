package com.ecommerce.service;

import com.ecommerce.dto.OrderRequest;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.*;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserService userService;
    private final ProductRepository productRepository;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    public List<Order> getOrdersByUser(Long userId) {
        userService.getUserById(userId); // validates user exists
        return orderRepository.findByUserId(userId);
    }

    @Transactional
    public Order placeOrder(OrderRequest request) {
        User user = userService.getUserById(request.getUserId());
        Order order = new Order(user, request.getShippingAddress());

        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + itemRequest.getProductId()));

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new BadRequestException(
                        "Insufficient stock for product: " + product.getName() +
                        ". Available: " + product.getStock() +
                        ", Requested: " + itemRequest.getQuantity());
            }

            OrderItem item = new OrderItem(order, product, itemRequest.getQuantity());
            order.getItems().add(item);

            // Deduct stock
            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);
        }

        order.calculateTotal();
        return orderRepository.save(order);
    }

    public Order updateOrderStatus(Long id, String status) {
        Order order = getOrderById(id);
        try {
            order.setStatus(Order.Status.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status +
                    ". Valid values: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED");
        }
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long id) {
        Order order = getOrderById(id);
        if (order.getStatus() == Order.Status.DELIVERED) {
            throw new BadRequestException("Cannot cancel a delivered order");
        }
        // Restore stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
        order.setStatus(Order.Status.CANCELLED);
        return orderRepository.save(order);
    }
}
