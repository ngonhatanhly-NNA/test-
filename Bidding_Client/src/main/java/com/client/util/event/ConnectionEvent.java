package com.client.util.event;

public class ConnectionEvent {
    public final boolean isConnected;
    public final String message;

    public ConnectionEvent(boolean isConnected, String message) {
        this.isConnected = isConnected;
        this.message = message;
    }
}