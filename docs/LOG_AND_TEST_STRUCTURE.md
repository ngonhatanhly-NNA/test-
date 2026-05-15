# 📋 Log Structure & Testing Architecture

## 1️⃣ LOG DIRECTORY STRUCTURE

```
Team13_Bidding_System/
├── logs/
│   ├── server.log              # Main server application log
│   ├── client.log              # Main client application log
│   │
│   ├── realtime/               # Realtime updates logs
│   │   ├── auction-updates.log # AUCTION_UPDATE messages
│   │   ├── auction-created.log # AUCTION_CREATED events
│   │   ├── auction-finished.log# AUCTION_FINISHED events
│   │   └── websocket.log       # WebSocket connection/message logs
│   │
│   ├── bidding/                # Bidding operations logs
│   │   ├── bid-placement.log   # Bid placement attempts & results
│   │   ├── auto-bid.log        # Auto-bidding activity
│   │   ├── validation.log      # Bid validation results
│   │   └── anti-sniping.log    # Anti-sniping events
│   │
│   ├── items/                  # Item management logs
│   │   ├── item-create.log     # Item creation events
│   │   ├── item-update.log     # Item modifications
│   │   └── image-upload.log    # Image upload/processing
│   │
│   ├── auction/                # Auction lifecycle logs
│   │   ├── auction-create.log  # Auction creation
│   │   ├── auction-schedule.log# Auction scheduling (start/end)
│   │   └── auction-finish.log  # Auction completion
│   │
│   ├── users/                  # User management logs
│   │   ├── auth.log            # Authentication/login/register
│   │   ├── profile.log         # Profile updates
│   │   └── role-changes.log    # Role/permission changes
│   │
│   ├── errors/                 # Error logs
│   │   ├── exceptions.log      # Uncaught exceptions
│   │   ├── validation-errors.log # Validation failures
│   │   └── network-errors.log  # Network/WebSocket errors
│   │
│   ├── performance/            # Performance monitoring
│   │   ├── response-times.log  # API response times
│   │   ├── database-queries.log# Query performance
│   │   └── thread-pool-stats.log # Thread execution stats
│   │
│   └── debug/                  # Debug logs (dev only)
│       ├── trace.log           # Detailed trace logs
│       └── cache-hits.log      # Cache performance
```

## 2️⃣ LOG CONFIGURATION (logback.xml)

### Server-side: `Bidding_Server/src/main/resources/logback.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Define appenders for different log categories -->
    
    <!-- ROOT LOGGER -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%-5level] [%thread] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- SERVER APPLICATION LOG -->
    <appender name="SERVER_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/server.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%thread] %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/server-%d{yyyy-MM-dd}-%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- REALTIME UPDATES -->
    <appender name="AUCTION_UPDATES" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/realtime/auction-updates.log</file>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [AuctionId:%X{auctionId}] [Bidder:%X{bidderId}] Price:%X{price} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/realtime/auction-updates-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
    </appender>

    <appender name="WEBSOCKET" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/realtime/websocket.log</file>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/realtime/websocket-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- BIDDING OPERATIONS -->
    <appender name="BID_PLACEMENT" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/bidding/bid-placement.log</file>
        <encoder>
            <pattern>%d{HH:mm:ss} [Auction:%X{auctionId}] [Bidder:%X{bidderId}] Amount:%X{amount} Status:%X{status} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/bidding/bid-placement-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- ERROR HANDLING -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/errors/exceptions.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] %logger{36} - %msg%n%ex</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/errors/exceptions-%d{yyyy-MM-dd}-%i.log</fileNamePattern>
            <maxFileSize>50MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- CONFIGURE LOGGERS FOR SPECIFIC PACKAGES -->
    <logger name="com.server.service.AuctionService" level="DEBUG" additivity="false">
        <appender-ref ref="AUCTION_UPDATES"/>
    </logger>

    <logger name="com.server.websocket.Broadcaster" level="INFO" additivity="false">
        <appender-ref ref="WEBSOCKET"/>
    </logger>

    <logger name="com.server.service.auction.processor" level="DEBUG" additivity="false">
        <appender-ref ref="BID_PLACEMENT"/>
    </logger>

    <!-- ROOT LOGGER CONFIGURATION -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="SERVER_FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </root>
</configuration>
```

### Client-side: `Bidding_Client/src/main/resources/logback.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%-5level] %logger{20} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="CLIENT_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/client.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%thread] %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/client-%d{yyyy-MM-dd}-%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <appender name="REALTIME_UPDATES" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/realtime/client-updates.log</file>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [AuctionId:%X{auctionId}] %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/realtime/client-updates-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
    </appender>

    <logger name="com.client.network.MyWebSocketClient" level="DEBUG" additivity="false">
        <appender-ref ref="REALTIME_UPDATES"/>
    </logger>

    <logger name="com.client.controller.dashboard.ViewLiveAuctions" level="DEBUG" additivity="false">
        <appender-ref ref="REALTIME_UPDATES"/>
    </logger>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="CLIENT_FILE"/>
    </root>
</configuration>
```

## 3️⃣ LOGGING IN CODE

### Server-side Usage:

```java
// AuctionService.java
private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);

public AuctionUpdateDTO placeBid(BidRequestDTO request) {
    // Use MDC for contextual logging
    MDC.put("auctionId", String.valueOf(request.getAuctionId()));
    MDC.put("bidderId", String.valueOf(request.getBidderId()));
    MDC.put("amount", request.getBidAmount().toPlainString());
    
    try {
        logger.info("Placing bid for auction");
        // ... bidding logic ...
        logger.info("Bid placed successfully");
        MDC.put("status", "SUCCESS");
    } catch (AuctionException e) {
        logger.error("Bid placement failed: {}", e.getMessage());
        MDC.put("status", "FAILED");
    } finally {
        MDC.clear();
    }
}

public void onAuctionUpdate(AuctionUpdateDTO update) {
    MDC.put("auctionId", String.valueOf(update.getAuctionId()));
    logger.info("Broadcasting update - Price: {}, Leader: {}", 
               update.getCurrentPrice(), update.getHighestBidderName());
    MDC.clear();
}
```

### Client-side Usage:

```java
// ViewLiveAuctions.java
private static final Logger logger = Logger.getLogger(ViewLiveAuctions.class.getName());

public void updatePriceRealtime(AuctionUpdateDTO updateData) {
    logger.info("Received realtime update for auction " + updateData.getAuctionId());
    
    Platform.runLater(() -> {
        logger.fine("Updating UI for auction " + updateData.getAuctionId());
        updateCurrentAuctionDisplay(updateData);
        logger.info("UI updated successfully");
    });
}
```

## 4️⃣ TEST LOGS DIRECTORY

```
Bidding_Server/src/test/resources/
├── logback-test.xml            # Test-specific logging config
└── test-logs/                  # Test execution logs
    ├── realtime-tests.log      # Realtime auction tests
    ├── auction-tests.log       # General auction service tests
    └── integration-tests.log   # End-to-end integration tests
```

## 5️⃣ TESTING STRATEGY

### Unit Tests:
- ✅ RealtimeAuctionUpdateTest.java - Server-side realtime
- ✅ BroadcasterTest.java - WebSocket message sending
- ✅ MyWebSocketClientTest.java - Client message parsing
- ✅ ViewLiveAuctionsRealtimeTest.java - UI display logic

### Integration Tests:
```java
@Test
public void testEndToEndRealtimeFlow() {
    // 1. Create auction → Server broadcasts AUCTION_CREATED
    // 2. Place bid → Server sends AUCTION_UPDATE
    // 3. Client receives → Updates price chart
    // 4. Other clients see change → realtime confirmation
}
```

### Performance Tests:
```java
@Test
public void testRealtimeBroadcastLatency() {
    // Measure time from bid placement → client receives update
    // Should be < 100ms for good UX
}

@Test
public void testMultipleAuctionUpdatesPerSecond() {
    // Test with 10+ auctions each receiving 5+ updates/sec
    // Ensure no message loss or UI lag
}
```

## 6️⃣ LOG ANALYSIS COMMANDS

```bash
# Check realtime message frequency
tail -f logs/realtime/auction-updates.log | grep "AUCTION_UPDATE"

# Monitor WebSocket connections
tail -f logs/realtime/websocket.log | grep "onOpen\|onClose"

# Find slow responses
grep "took" logs/performance/response-times.log | sort -t: -k2 -n

# Count successful bids per hour
grep "Bid placed successfully" logs/bidding/bid-placement.log | cut -d' ' -f1 | sort | uniq -c

# Find errors
grep "ERROR" logs/errors/exceptions.log | tail -20
```

## 7️⃣ MONITORING CHECKLIST

### Realtime Health:
- [ ] Broadcaster receives updates from all active auctions
- [ ] Clients receive messages within 100ms
- [ ] No messages dropped when clients connect/disconnect
- [ ] Multiple concurrent updates processed correctly

### UI Display:
- [ ] Price updates reflect immediately on client
- [ ] Leader name changes instantly
- [ ] Remaining time counts down smoothly
- [ ] No UI freezes during updates

### Error Handling:
- [ ] Malformed messages logged (not crashing)
- [ ] Closed clients skipped in broadcast
- [ ] Connection loss triggers reconnect
- [ ] Large JSON payloads handled correctly


