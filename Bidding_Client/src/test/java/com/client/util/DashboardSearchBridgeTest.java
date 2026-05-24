package com.client.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DashboardSearchBridge Tests")
public class DashboardSearchBridgeTest {

    @BeforeEach
    void setUp() {
        DashboardSearchBridge.setOnSearch(null);
    }

    @Test
    void testPublishNoListenerNoThrow() {
        assertDoesNotThrow(() -> DashboardSearchBridge.publish("hello"));
    }

    @Test
    void testPublishCallsListener() {
        List<String> received = new ArrayList<>();
        DashboardSearchBridge.setOnSearch(received::add);
        DashboardSearchBridge.publish("auction");
        assertEquals(1, received.size());
        assertEquals("auction", received.get(0));
    }

    @Test
    void testPublishNullSendsEmpty() {
        List<String> received = new ArrayList<>();
        DashboardSearchBridge.setOnSearch(received::add);
        DashboardSearchBridge.publish(null);
        assertEquals("", received.get(0));
    }

    @Test
    void testSetOnSearchReplacesPrevious() {
        List<String> first = new ArrayList<>(), second = new ArrayList<>();
        DashboardSearchBridge.setOnSearch(first::add);
        DashboardSearchBridge.setOnSearch(second::add);
        DashboardSearchBridge.publish("test");
        assertTrue(first.isEmpty());
        assertEquals(1, second.size());
    }

    @Test
    void testPublishMultipleTimes() {
        List<String> received = new ArrayList<>();
        DashboardSearchBridge.setOnSearch(received::add);
        DashboardSearchBridge.publish("q1");
        DashboardSearchBridge.publish("q2");
        DashboardSearchBridge.publish("q3");
        assertEquals(3, received.size());
    }

    @Test
    void testPublishEmptyString() {
        List<String> received = new ArrayList<>();
        DashboardSearchBridge.setOnSearch(received::add);
        DashboardSearchBridge.publish("");
        assertEquals("", received.get(0));
    }
}