package com.client.network;

import com.client.session.ClientSession;
import com.shared.dto.UserProfileResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NetworkClient Tests")
public class NetworkClientTest {

    @BeforeEach
    void setUp() {
        ClientSession.clear();
    }

    @Test
    void testGetInstanceNotNull() {
        assertNotNull(NetworkClient.getInstance());
    }

    @Test
    void testGetInstanceSingleton() {
        assertSame(NetworkClient.getInstance(), NetworkClient.getInstance());
    }

    @Test
    void testNewRequestBuilderCorrectUri() throws Exception {
        URI uri = new URI("http://localhost:8080/api/test");
        HttpRequest req = NetworkClient.newRequestBuilder(uri).GET().build();
        assertEquals(uri, req.uri());
    }

    @Test
    void testAuthHeaderPresentWhenTokenSet() throws Exception {
        UserProfileResponseDTO p = new UserProfileResponseDTO(
                1L, "user", "e@e.com", "Full", "0", "HN", "BIDDER", 0.0, null);
        ClientSession.setSession(p, "abc-token");

        HttpRequest req = NetworkClient.newRequestBuilder(
                new URI("http://localhost:8080/secure")).GET().build();

        assertTrue(req.headers().firstValue("Authorization").isPresent());
        assertEquals("Bearer abc-token",
                req.headers().firstValue("Authorization").get());
    }

    @Test
    void testNoAuthHeaderWhenNoToken() throws Exception {
        ClientSession.clear();
        HttpRequest req = NetworkClient.newRequestBuilder(
                new URI("http://localhost:8080/public")).GET().build();
        assertFalse(req.headers().firstValue("Authorization").isPresent());
    }

    @Test
    void testClearCookiesNoThrow() {
        assertDoesNotThrow(NetworkClient::clearCookies);
    }
}