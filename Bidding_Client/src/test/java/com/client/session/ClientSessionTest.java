package com.client.session;

import com.shared.dto.UserProfileResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClientSession Tests")
public class ClientSessionTest {

    @BeforeEach
    void setUp() {
        ClientSession.clear();
    }

    private UserProfileResponseDTO makeProfile(long id, String username, String role) {
        return new UserProfileResponseDTO(id, username, "email@test.com",
                "Full Name", "0123456789", "Hanoi", role, 0.0);
    }

    @Test
    void testInitialStateNotLoggedIn() {
        assertFalse(ClientSession.isLoggedIn());
        assertEquals(0L, ClientSession.getUserId());
        assertEquals("", ClientSession.getUsername());
        assertEquals("", ClientSession.getRole());
        assertEquals("", ClientSession.getToken());
    }

    @Test
    void testSetUserSetsFields() {
        ClientSession.setUser(makeProfile(42L, "alice", "BIDDER"));
        assertEquals(42L, ClientSession.getUserId());
        assertEquals("alice", ClientSession.getUsername());
        assertEquals("BIDDER", ClientSession.getRole());
        assertTrue(ClientSession.isLoggedIn());
    }

    @Test
    void testSetSessionSetsUserAndToken() {
        ClientSession.setSession(makeProfile(10L, "bob", "SELLER"), "mytoken123");
        assertEquals(10L, ClientSession.getUserId());
        assertEquals("mytoken123", ClientSession.getToken());
        assertTrue(ClientSession.isLoggedIn());
    }

    @Test
    void testSetUserNullClearsSession() {
        ClientSession.setUser(makeProfile(5L, "user", "BIDDER"));
        ClientSession.setUser(null);
        assertFalse(ClientSession.isLoggedIn());
        assertEquals("", ClientSession.getUsername());
    }

    @Test
    void testSetSessionNullTokenStoresEmpty() {
        ClientSession.setSession(makeProfile(7L, "charlie", "ADMIN"), null);
        assertEquals("", ClientSession.getToken());
    }

    @Test
    void testSetUserNullUsernameStoresEmpty() {
        UserProfileResponseDTO p = new UserProfileResponseDTO(
                3L, null, null, null, null, null, null, 0.0);
        ClientSession.setUser(p);
        assertEquals("", ClientSession.getUsername());
        assertEquals("", ClientSession.getRole());
        assertTrue(ClientSession.isLoggedIn());
    }

    @Test
    void testClearResetsAll() {
        ClientSession.setSession(makeProfile(99L, "admin", "ADMIN"), "token999");
        ClientSession.clear();
        assertFalse(ClientSession.isLoggedIn());
        assertEquals(0L, ClientSession.getUserId());
        assertEquals("", ClientSession.getToken());
    }

    @Test
    void testIsLoggedInFalseWhenIdZero() {
        ClientSession.setUser(makeProfile(0L, "ghost", "BIDDER"));
        assertFalse(ClientSession.isLoggedIn());
    }

    @Test
    void testMultipleSetUserOverwrites() {
        ClientSession.setUser(makeProfile(1L, "first", "BIDDER"));
        ClientSession.setUser(makeProfile(2L, "second", "SELLER"));
        assertEquals(2L, ClientSession.getUserId());
        assertEquals("second", ClientSession.getUsername());
        assertEquals("SELLER", ClientSession.getRole());
    }

    @Test
    void testGetRoleAfterSet() {
        ClientSession.setUser(makeProfile(5L, "admin", "ADMIN"));
        assertEquals("ADMIN", ClientSession.getRole());
    }
}