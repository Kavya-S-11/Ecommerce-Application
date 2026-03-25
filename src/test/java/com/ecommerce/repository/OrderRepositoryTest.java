package com.ecommerce.repository;

import com.ecommerce.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private User alice;
    private User bob;
    private Order order1;
    private Order order2;
    private Order order3;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userRepository.deleteAll();

        alice = userRepository.save(new User("Alice", "alice@example.com", "pass", "123 Main St"));
        bob = userRepository.save(new User("Bob", "bob@example.com", "pass", "456 Oak Ave"));

        order1 = new Order(alice, "123 Main St");
        order1.setStatus(Order.Status.PENDING);
        order1 = orderRepository.save(order1);

        order2 = new Order(alice, "123 Main St");
        order2.setStatus(Order.Status.CONFIRMED);
        order2 = orderRepository.save(order2);

        order3 = new Order(bob, "456 Oak Ave");
        order3.setStatus(Order.Status.PENDING);
        order3 = orderRepository.save(order3);
    }

    @Test
    void findAll_returnsAllOrders() {
        List<Order> result = orderRepository.findAll();
        assertThat(result).hasSize(3);
    }

    @Test
    void findById_existingId_returnsOrder() {
        Optional<Order> result = orderRepository.findById(order1.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getUser().getName()).isEqualTo("Alice");
    }

    @Test
    void findById_nonExistingId_returnsEmpty() {
        Optional<Order> result = orderRepository.findById(999L);
        assertThat(result).isEmpty();
    }

    @Test
    void findByUserId_aliceOrders_returnsTwoOrders() {
        List<Order> result = orderRepository.findByUserId(alice.getId());
        assertThat(result).hasSize(2);
    }

    @Test
    void findByUserId_bobOrders_returnsOneOrder() {
        List<Order> result = orderRepository.findByUserId(bob.getId());
        assertThat(result).hasSize(1);
    }

    @Test
    void findByUserId_nonExistingUser_returnsEmpty() {
        List<Order> result = orderRepository.findByUserId(999L);
        assertThat(result).isEmpty();
    }

    @Test
    void findByStatus_pending_returnsTwoPendingOrders() {
        List<Order> result = orderRepository.findByStatus(Order.Status.PENDING);
        assertThat(result).hasSize(2);
    }

    @Test
    void findByStatus_confirmed_returnsOneOrder() {
        List<Order> result = orderRepository.findByStatus(Order.Status.CONFIRMED);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getName()).isEqualTo("Alice");
    }

    @Test
    void findByStatus_shipped_returnsEmpty() {
        List<Order> result = orderRepository.findByStatus(Order.Status.SHIPPED);
        assertThat(result).isEmpty();
    }

    @Test
    void save_persistsOrder() {
        Order newOrder = new Order(bob, "789 Pine Rd");
        Order saved = orderRepository.save(newOrder);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(Order.Status.PENDING);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void updateStatus_changesOrderStatus() {
        order1.setStatus(Order.Status.SHIPPED);
        orderRepository.save(order1);

        Optional<Order> updated = orderRepository.findById(order1.getId());
        assertThat(updated.get().getStatus()).isEqualTo(Order.Status.SHIPPED);
    }

    @Test
    void delete_removesOrder() {
        orderRepository.delete(order3);
        Optional<Order> result = orderRepository.findById(order3.getId());
        assertThat(result).isEmpty();
    }
}
