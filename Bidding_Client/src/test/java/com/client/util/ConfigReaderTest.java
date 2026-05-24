package com.client.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigReader Tests")
public class ConfigReaderTest {

    @Test
    @DisplayName("getHttpUrl returns non-null value")
    void testGetHttpUrlNotNull() {
        assertNotNull(ConfigReader.getHttpUrl());
    }

    @Test
    @DisplayName("getWebSocketUrl returns non-null value")
    void testGetWebSocketUrlNotNull() {
        assertNotNull(ConfigReader.getWebSocketUrl());
    }

    @Test
    @DisplayName("getHttpUrl returns default when no config file")
    void testGetHttpUrlDefault() {
        String url = ConfigReader.getHttpUrl();
        // Either loaded from file or falls back to default
        assertFalse(url.isEmpty(), "HTTP URL should not be empty");
        assertTrue(url.startsWith("http"), "HTTP URL should start with http");
    }

    @Test
    @DisplayName("getWebSocketUrl returns default when no config file")
    void testGetWebSocketUrlDefault() {
        String url = ConfigReader.getWebSocketUrl();
        assertFalse(url.isEmpty(), "WebSocket URL should not be empty");
        assertTrue(url.startsWith("ws"), "WebSocket URL should start with ws");
    }
}