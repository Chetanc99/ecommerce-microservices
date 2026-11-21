package com.ecommerce.server.service;

import com.ecommerce.server.entity.User;
import com.ecommerce.server.exception.DuplicateEmailException;
import com.ecommerce.server.exception.InvalidPasswordException;
import com.ecommerce.server.exception.UserNotFoundException;
import com.ecommerce.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registrationNormalizesEmailHashesPasswordAndForcesUserRole() {
        User candidate = new User("Ada Lovelace", "  ADA@Example.COM ", "plain-password", "ADMIN");

        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-password")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.register(candidate);

        assertThat(saved.getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getPassword()).isEqualTo("$2a$10$hashed");
        assertThat(saved.getRole()).isEqualTo("USER");
    }

    @Test
    void registrationRejectsDuplicateEmail() {
        when(userRepository.findByEmail("ada@example.com"))
                .thenReturn(Optional.of(new User("Ada", "ada@example.com", "hash", "USER")));

        assertThatThrownBy(() -> userService.register(
                new User("Ada", "ada@example.com", "plain-password", "USER")))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginAcceptsMatchingPassword() {
        User stored = new User("Ada", "ada@example.com", "$2a$10$hashed", "USER");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("secret", "$2a$10$hashed")).thenReturn(true);

        assertThat(userService.login(" ADA@example.com", "secret")).isSameAs(stored);
    }

    @Test
    void loginRejectsWrongPasswordWithoutRevealingWhichPartFailed() {
        User stored = new User("Ada", "ada@example.com", "$2a$10$hashed", "USER");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.login("ada@example.com", "wrong"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login("ghost@example.com", "secret"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void profileUpdateRejectsEmailOwnedByAnotherAccount() {
        User current = userWithId(1L, "ada@example.com");
        User other = userWithId(2L, "taken@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(current));
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.updateProfile(1L, "Ada", "taken@example.com"))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void profileUpdateKeepsOwnEmailAndTrimsName() {
        User current = userWithId(1L, "ada@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(current));
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(current));
        when(userRepository.save(current)).thenReturn(current);

        User updated = userService.updateProfile(1L, "  Ada Lovelace  ", " ADA@example.com ");

        assertThat(updated.getName()).isEqualTo("Ada Lovelace");
        assertThat(updated.getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void changePasswordRequiresTheCurrentPassword() {
        User current = userWithId(1L, "ada@example.com");
        current.setPassword("$2a$10$old");

        when(userRepository.findById(1L)).thenReturn(Optional.of(current));
        when(passwordEncoder.matches("wrong", "$2a$10$old")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, "wrong", "new-password"))
                .isInstanceOf(InvalidPasswordException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordStoresAHashOfTheNewPassword() {
        User current = userWithId(1L, "ada@example.com");
        current.setPassword("$2a$10$old");

        when(userRepository.findById(1L)).thenReturn(Optional.of(current));
        when(passwordEncoder.matches("old", "$2a$10$old")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("$2a$10$new");
        when(userRepository.save(current)).thenReturn(current);

        userService.changePassword(1L, "old", "new-password");

        assertThat(current.getPassword()).isEqualTo("$2a$10$new");
    }

    @Test
    void findByIdFailsForMissingUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void ensureAdminPromotesAnExistingAccount() {
        User existing = userWithId(3L, "admin@example.com");
        existing.setRole("USER");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        userService.ensureAdmin("ADMIN@example.com", "admin-password");

        assertThat(existing.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void ensureAdminDoesNothingWhenCredentialsAreAbsent() {
        userService.ensureAdmin("", "");

        verify(userRepository, never()).save(any());
    }

    private User userWithId(Long id, String email) {
        User user = new User("Ada", email, "hash", "USER");
        user.setId(id);
        return user;
    }
}
