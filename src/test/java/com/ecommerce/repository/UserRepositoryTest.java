package com.ecommerce.repository;

import com.ecommerce.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        alice = userRepository.save(new User("Alice Johnson", "alice@example.com", "pass123", "123 Main St"));
        bob = userRepository.save(new User("Bob Smith", "bob@example.com", "pass456", "456 Oak Ave"));
    }

    @Test
    void findAll_returnsAllUsers() {
        List<User> result = userRepository.findAll();
        assertThat(result).hasSize(2);
    }

    @Test
    void findById_existingId_returnsUser() {
        Optional<User> result = userRepository.findById(alice.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice Johnson");
    }

    @Test
    void findById_nonExistingId_returnsEmpty() {
        Optional<User> result = userRepository.findById(999L);
        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_existingEmail_returnsUser() {
        Optional<User> result = userRepository.findByEmail("alice@example.com");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice Johnson");
    }

    @Test
    void findByEmail_nonExistingEmail_returnsEmpty() {
        Optional<User> result = userRepository.findByEmail("unknown@example.com");
        assertThat(result).isEmpty();
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        boolean exists = userRepository.existsByEmail("bob@example.com");
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_nonExistingEmail_returnsFalse() {
        boolean exists = userRepository.existsByEmail("noone@example.com");
        assertThat(exists).isFalse();
    }

    @Test
    void save_persistsUser() {
        User charlie = new User("Charlie Brown", "charlie@example.com", "pass789", "789 Pine Rd");
        User saved = userRepository.save(charlie);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("charlie@example.com");
    }

    @Test
    void delete_removesUser() {
        userRepository.delete(alice);
        Optional<User> result = userRepository.findById(alice.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void update_changesAddress() {
        alice.setAddress("999 New Address");
        userRepository.save(alice);

        Optional<User> updated = userRepository.findById(alice.getId());
        assertThat(updated.get().getAddress()).isEqualTo("999 New Address");
    }
}
