package com.client.util.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EventBus Tests")
public class EventBusTest {

    private EventBus bus;

    @BeforeEach
    void setUp() {
        bus = EventBus.getInstance();
    }

    @Test
    void testSingletonInstance() {
        assertSame(EventBus.getInstance(), EventBus.getInstance());
    }

    @Test
    void testSubscribeAndPublishAuctionFinished() {
        List<Long> received = new ArrayList<>();
        bus.subscribe(AuctionFinishedEvent.class, e -> received.add(e.auctionId));
        bus.publish(new AuctionFinishedEvent(123L));
        assertTrue(received.contains(123L));
    }

    @Test
    void testSubscribeAndPublishConnection() {
        List<Boolean> states = new ArrayList<>();
        bus.subscribe(ConnectionEvent.class, e -> states.add(e.isConnected));
        bus.publish(new ConnectionEvent(true, "ok"));
        assertTrue(states.get(states.size() - 1));
    }

    @Test
    void testPublishNoSubscribersNoThrow() {
        assertDoesNotThrow(() -> bus.publish(new AuctionFinishedEvent(1L)));
    }

    @Test
    void testMultipleSubscribersAllReceive() {
        List<Long> r1 = new ArrayList<>(), r2 = new ArrayList<>();
        bus.subscribe(AuctionFinishedEvent.class, e -> r1.add(e.auctionId));
        bus.subscribe(AuctionFinishedEvent.class, e -> r2.add(e.auctionId));
        bus.publish(new AuctionFinishedEvent(77L));
        assertTrue(r1.contains(77L));
        assertTrue(r2.contains(77L));
    }

    @Test
    void testAuctionFinishedEventStoresId() {
        AuctionFinishedEvent e = new AuctionFinishedEvent(55L);
        assertEquals(55L, e.auctionId);
    }

    @Test
    void testConnectionEventStoresFields() {
        ConnectionEvent e = new ConnectionEvent(false, "Disconnected");
        assertFalse(e.isConnected);
        assertEquals("Disconnected", e.message);
    }

    @Test
    void testDifferentEventTypesDoNotInterfere() {
        List<Object> fin = new ArrayList<>(), con = new ArrayList<>();
        bus.subscribe(AuctionFinishedEvent.class, fin::add);
        bus.subscribe(ConnectionEvent.class, con::add);
        bus.publish(new AuctionFinishedEvent(1L));
        bus.publish(new ConnectionEvent(true, "up"));
        assertFalse(fin.isEmpty());
        assertFalse(con.isEmpty());
    }
}