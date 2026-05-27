package com.server.service;

import com.server.DAO.UserRepository;
import com.server.exception.AppException;
import com.server.exception.AuthValidationException;
import com.server.exception.DuplicateUserException;
import com.server.exception.InvalidCredentialException;
import com.server.exception.UserNotFoundException;
import com.server.model.Bidder;
import com.server.model.Status;
import com.server.model.User;
import com.shared.dto.BaseProfileUpdateDTO;
import com.shared.dto.LoginRequestDTO;
import com.shared.dto.RegisterRequestDTO;
import com.shared.dto.UserProfileResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private RegisterRequestDTO registerRequest;
    private LoginRequestDTO loginRequest;
    private User userInDb;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequestDTO("testuser", "password", "test@test.com", "Test User");
        loginRequest = new LoginRequestDTO("testuser", "password");
        userInDb = new Bidder("testuser", "$2a$10$vI8v.H.1.y.p.e.r.s.e.c.u.r.e.P.a.s.s", "test@test.com", "Test User");
        userInDb.updateStatus(Status.ACTIVE);
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {
        @Test
        void register_shouldSucceed_withValidData() {
            when(userRepository.getUserByUsername(anyString())).thenReturn(null);
            when(userRepository.saveUser(any(User.class))).thenReturn(true);
            assertDoesNotThrow(() -> authService.register(registerRequest));
        }

        @Test
        void register_shouldThrowException_whenSaveFails() {
            when(userRepository.getUserByUsername(anyString())).thenReturn(null);
            when(userRepository.saveUser(any(User.class))).thenReturn(false);
            assertThrows(AppException.class, () -> authService.register(registerRequest));
        }

        @Test
        void register_shouldThrowException_whenUsernameExists() {
            when(userRepository.getUserByUsername("testuser")).thenReturn(new Bidder());
            assertThrows(DuplicateUserException.class, () -> authService.register(registerRequest));
        }

        @Test
        void register_shouldThrowException_whenDataIsInvalid() {
            assertThrows(AuthValidationException.class, () -> authService.register(null));
            assertThrows(AuthValidationException.class, () -> authService.register(new RegisterRequestDTO(" ", "p", "e", "f")));
            assertThrows(AuthValidationException.class, () -> authService.register(new RegisterRequestDTO("u", " ", "e", "f")));
            assertThrows(AuthValidationException.class, () -> authService.register(new RegisterRequestDTO("u", "p", " ", "f")));
            assertThrows(AuthValidationException.class, () -> authService.register(new RegisterRequestDTO("u", "p", "e", " ")));
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {
        @Test
        void login_shouldSucceed_withValidCredentials() {
            User user = new Bidder("testuser", "password", "test@test.com", "Test User");
            user.updateStatus(Status.ACTIVE);
            
            when(userRepository.getUserByUsername("testuser")).thenReturn(user);
            UserProfileResponseDTO profile = authService.login(loginRequest);
            assertNotNull(profile);
            assertEquals("testuser", profile.getUsername());
        }

        @Test
        void login_shouldThrowException_whenUserNotFound() {
            when(userRepository.getUserByUsername("unknown")).thenReturn(null);
            assertThrows(UserNotFoundException.class, () -> authService.login(new LoginRequestDTO("unknown", "password")));
        }

        @Test
        void login_shouldThrowException_whenPasswordIsInvalid() {
            when(userRepository.getUserByUsername("testuser")).thenReturn(userInDb);
            LoginRequestDTO wrongLogin = new LoginRequestDTO("testuser", "wrongpassword");
            assertThrows(InvalidCredentialException.class, () -> authService.login(wrongLogin));
        }

        @Test
        void login_shouldThrowException_whenAccountIsBanned() {
            userInDb.updateStatus(Status.BANNED);
            when(userRepository.getUserByUsername("testuser")).thenReturn(userInDb);
            assertThrows(AuthValidationException.class, () -> authService.login(loginRequest));
        }

        @Test
        void login_shouldThrowException_whenDataIsInvalid() {
            assertThrows(AuthValidationException.class, () -> authService.login(null));
            assertThrows(AuthValidationException.class, () -> authService.login(new LoginRequestDTO("", "password")));
            assertThrows(AuthValidationException.class, () -> authService.login(new LoginRequestDTO("user", "")));
        }
    }

    @Nested
    @DisplayName("Profile Tests")
    class ProfileTests {
        @Test
        void getUserProfile_shouldReturnProfile_whenUserExists() {
            when(userRepository.getUserByUsername("testuser")).thenReturn(userInDb);
            UserProfileResponseDTO profile = authService.getUserProfile("testuser");
            assertNotNull(profile);
            assertEquals("testuser", profile.getUsername());
        }

        @Test
        void getUserProfile_shouldThrowException_whenUserNotFound() {
            when(userRepository.getUserByUsername("unknown")).thenReturn(null);
            assertThrows(UserNotFoundException.class, () -> authService.getUserProfile("unknown"));
        }

        @Test
        void getUserProfile_shouldThrowException_whenUsernameIsBlank() {
            assertThrows(AuthValidationException.class, () -> authService.getUserProfile(" "));
        }
    }

    @Nested
    @DisplayName("Update Profile Tests")
    class UpdateProfileTests {
        @Test
        void updateProfile_shouldSucceed() {
            when(userRepository.getUserById(1L)).thenReturn(userInDb);
            when(userRepository.updateUser(any(User.class))).thenReturn(true);
            BaseProfileUpdateDTO dto = new BaseProfileUpdateDTO();
            dto.setId(1L);
            assertDoesNotThrow(() -> authService.updateProfile(dto));
        }

        @Test
        void updateProfile_shouldThrowException_whenUserNotFound() {
            when(userRepository.getUserById(1L)).thenReturn(null);
            BaseProfileUpdateDTO dto = new BaseProfileUpdateDTO();
            dto.setId(1L);
            assertThrows(UserNotFoundException.class, () -> authService.updateProfile(dto));
        }

        @Test
        void updateProfile_shouldThrowException_whenUpdateFails() {
            when(userRepository.getUserById(1L)).thenReturn(userInDb);
            when(userRepository.updateUser(any(User.class))).thenReturn(false);
            BaseProfileUpdateDTO dto = new BaseProfileUpdateDTO();
            dto.setId(1L);
            assertThrows(AppException.class, () -> authService.updateProfile(dto));
        }

        @Test
        void updateProfile_shouldThrowException_whenDataIsNull() {
            assertThrows(AuthValidationException.class, () -> authService.updateProfile(null));
            BaseProfileUpdateDTO dto = new BaseProfileUpdateDTO();
            dto.setId(0L);
            assertThrows(AuthValidationException.class, () -> authService.updateProfile(dto));
        }
    }
}