package com.server.service;

import com.server.DAO.UserRepository;
import com.server.exception.AuthValidationException;
import com.server.exception.DuplicateUserException;
import com.server.exception.InvalidCredentialException;
import com.server.exception.UserNotFoundException;
import com.server.model.Bidder;
import com.shared.dto.LoginRequestDTO;
import com.shared.dto.RegisterRequestDTO;
import com.shared.network.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthServiceTest {

    @Test
    void register_blankUsername_throwsValidation() {
        UserRepository repo = mock(UserRepository.class);
        AuthService service = new AuthService(repo);

        RegisterRequestDTO dto = new RegisterRequestDTO("  ", "pass", "a@b.com", "A");

        assertThrows(AuthValidationException.class, () -> service.register(dto));
    }

    @Test
    void register_existingUsername_throwsDuplicateUser() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserByUsername("u")).thenReturn(new Bidder("u", "p", "e@e.com", "n"));
        AuthService service = new AuthService(repo);

        RegisterRequestDTO dto = new RegisterRequestDTO("u", "p", "e@e.com", "n");

        assertThrows(DuplicateUserException.class, () -> service.register(dto));
    }

    @Test
    void login_userNotFound_throwsUserNotFound() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserByUsername("u")).thenReturn(null);
        AuthService service = new AuthService(repo);

        LoginRequestDTO dto = new LoginRequestDTO("u", "p");

        assertThrows(UserNotFoundException.class, () -> service.login(dto));
    }

    @Test
    void login_wrongPassword_throwsInvalidCredential() {
        UserRepository repo = mock(UserRepository.class);
        Bidder user = new Bidder("u", "correct", "e@e.com", "n");
        when(repo.getUserByUsername("u")).thenReturn(user);
        AuthService service = new AuthService(repo);

        LoginRequestDTO dto = new LoginRequestDTO("u", "wrong");

        assertThrows(InvalidCredentialException.class, () -> service.login(dto));
    }

    @Test
    void login_success_returnsProfile() {
        UserRepository repo = mock(UserRepository.class);
        Bidder user = new Bidder("u", "p", "e@e.com", "n");
        when(repo.getUserByUsername("u")).thenReturn(user);
        AuthService service = new AuthService(repo);

        LoginRequestDTO dto = new LoginRequestDTO("u", "p");

        Response res = service.login(dto);
        assertEquals("SUCCESS", res.getStatus());
        assertNotNull(res.getData());
    }
}

