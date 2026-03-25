package com.ecommerce.service;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.User;
import com.ecommerce.repository.UserRepository;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = new User("Alice Johnson", "alice@example.com", "pass123", "123 Main St");
        alice.setId(1L);

        bob = new User("Bob Smith", "bob@example.com", "pass456", "456 Oak Ave");
        bob.setId(2L);
    }

    // --- getAllUsers ---

    @Test
    void getAllUsers_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(alice, bob));

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getName).containsExactlyInAnyOrder("Alice Johnson", "Bob Smith");
    }

    @Test
    void getAllUsers_emptyRepo_returnsEmpty() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<User> result = userService.getAllUsers();

        assertThat(result).isEmpty();
    }

    // --- getUserById ---

    @Test
    void getUserById_existingId_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));

        User result = userService.getUserById(1L);

        assertThat(result.getName()).isEqualTo("Alice Johnson");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void getUserById_nonExistingId_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- registerUser ---

    @Test
    void registerUser_newEmail_savesAndReturnsUser() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(alice)).thenReturn(alice);

        User result = userService.registerUser(alice);

        assertThat(result.getName()).isEqualTo("Alice Johnson");
        verify(userRepository).save(alice);
    }

    @Test
    void registerUser_duplicateEmail_throwsBadRequestException() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(alice))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any());
    }

    // --- updateUser ---

    @Test
    void updateUser_existingId_updatesNameAndAddress() {
        User updated = new User("Alice Updated", "alice@example.com", "pass", "999 New St");
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(1L, updated);

        assertThat(result.getName()).isEqualTo("Alice Updated");
        assertThat(result.getAddress()).isEqualTo("999 New St");
    }

    @Test
    void updateUser_emailNotChanged() {
        User updated = new User("Alice Updated", "newemail@example.com", "pass", "999 New St");
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(1L, updated);

        // Email should not be updated (service only updates name and address)
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void updateUser_nonExistingId_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, alice))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    // --- deleteUser ---

    @Test
    void deleteUser_existingId_deletesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));

        userService.deleteUser(1L);

        verify(userRepository).delete(alice);
    }

    @Test
    void deleteUser_nonExistingId_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any());
    }
}
