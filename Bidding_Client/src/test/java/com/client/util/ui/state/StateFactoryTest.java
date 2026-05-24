package com.client.util.ui.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StateFactory Tests")
public class StateFactoryTest {

    @Test
    void testActiveStatus() {
        assertInstanceOf(ActiveAuctionState.class, StateFactory.getButtonState("ACTIVE"));
    }

    @Test
    void testFinishedStatus() {
        assertInstanceOf(FinishedAuctionState.class, StateFactory.getButtonState("FINISHED"));
    }

    @Test
    void testUnknownDefaultsToNotStarted() {
        assertInstanceOf(NotStartedAuctionState.class, StateFactory.getButtonState("UNKNOWN"));
    }

    @Test
    void testNullDefaultsToNotStarted() {
        assertInstanceOf(NotStartedAuctionState.class, StateFactory.getButtonState(null));
    }

    @Test
    void testEmptyDefaultsToNotStarted() {
        assertInstanceOf(NotStartedAuctionState.class, StateFactory.getButtonState(""));
    }

    @Test
    void testLowercaseActiveDefaultsToNotStarted() {
        // case-sensitive: "active" != "ACTIVE"
        assertInstanceOf(NotStartedAuctionState.class, StateFactory.getButtonState("active"));
    }

    @Test
    void testNewInstanceEachCall() {
        assertNotSame(StateFactory.getButtonState("ACTIVE"), StateFactory.getButtonState("ACTIVE"));
    }

    @Test
    void testActiveStateNotNull() {
        assertNotNull(new ActiveAuctionState());
    }

    @Test
    void testFinishedStateNotNull() {
        assertNotNull(new FinishedAuctionState());
    }

    @Test
    void testNotStartedStateNotNull() {
        assertNotNull(new NotStartedAuctionState());
    }
}