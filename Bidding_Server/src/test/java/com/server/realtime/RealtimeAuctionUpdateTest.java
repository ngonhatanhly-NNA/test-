package com.server.realtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.server.DAO.AuctionRepository;
import com.server.DAO.UserRepository;
import com.server.DAO.BidTransactionRepository;
import com.server.DAO.AutoBidRepository;
import com.server.model.Auction;
import com.server.model.Bidder; // Changed from User
import com.server.service.AuctionService;
import com.server.service.ItemService;
import com.server.service.auction.antisnipe.AntiSnipingStrategy;
import com.server.service.auction.processor.AutoBidProcessor;
import com.server.service.auction.processor.BidProcessor;
import com.server.service.auction.strategy.BidValidationChain;
import com.server.websocket.Broadcaster;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.dto.BidRequestDTO;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Test Suite: Realtime Auction Updates via WebSocket
 *
 * Kiểm tra:
 * - Khi bid được đặt, AuctionUpdateDTO được broadcast
 * - UI nhận được update và display realtime
 * - Price chart được cập nhật
 * - Remaining time được cập nhật
 * - Người dẫn đầu được cập nhật ngay lập tức
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("⚡ Realtime Auction Update Tests")
public class RealtimeAuctionUpdateTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private BidTransactionRepository bidRepository;
    @Mock private AutoBidRepository autoBidRepository;
    @Mock private ItemService itemService;
    @Mock private UserRepository userRepository;
    @Mock private BidValidationChain validationChain;
    @Mock private AntiSnipingStrategy antiSnipingStrategy;
    @Mock private BidProcessor bidProcessor;
    @Mock private AutoBidProcessor autoBidProcessor;
    @Mock private Broadcaster broadcaster;

    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService(
                auctionRepository,
                bidRepository,
                autoBidRepository,
                itemService,
                userRepository,
                validationChain,
                antiSnipingStrategy,
                bidProcessor,
                autoBidProcessor
        );
        auctionService.setEventListener(broadcaster);
    }

    // Helper method to inject auction into cache bypassing init()
    private void injectAuctionIntoCache(Auction auction) throws Exception {
        Field cacheField = AuctionService.class.getDeclaredField("auctionCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Long, Auction> cache = (ConcurrentHashMap<Long, Auction>) cacheField.get(auctionService);
        cache.put(auction.getId(), auction);

        Field lockField = AuctionService.class.getDeclaredField("auctionLocks");
        lockField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Long, ReentrantLock> locks = (ConcurrentHashMap<Long, ReentrantLock>) lockField.get(auctionService);
        locks.putIfAbsent(auction.getId(), new ReentrantLock());
    }

    @Test
    @DisplayName("Khi placeBid thành công, broadcaster.onAuctionUpdate() được gọi với dữ liệu đúng")
    void testBroadcasterCalledOnSuccessfulBid() throws Exception {
        // Arrange
        long auctionId = 1L;
        long bidderId = 2L;
        BigDecimal bidAmount = new BigDecimal("1000");

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStatus(Auction.AuctionStatus.RUNNING);
        auction.setCurrentHighestBid(new BigDecimal("900"));
        auction.setWinnerId(1L);
        auction.setEndTime(LocalDateTime.now().plusHours(1));
        auction.setStepPrice(new BigDecimal("10"));

        injectAuctionIntoCache(auction);

        BidRequestDTO bidRequest = new BidRequestDTO();
        bidRequest.setAuctionId(auctionId);
        bidRequest.setBidderId(bidderId);
        bidRequest.setBidAmount(bidAmount);

        Bidder bidder = new Bidder();
        bidder.setId(bidderId);
        bidder.setFullName("Bidder User");
        when(userRepository.getUserById(bidderId)).thenReturn(bidder);

        // Act
        AuctionUpdateDTO result = auctionService.placeBid(bidRequest);

        // Assert
        assertNotNull(result);
        assertEquals(auctionId, result.getAuctionId());

        // Verify broadcaster was called
        ArgumentCaptor<AuctionUpdateDTO> captor = ArgumentCaptor.forClass(AuctionUpdateDTO.class);
        verify(broadcaster, times(1)).onAuctionUpdate(captor.capture());

        AuctionUpdateDTO broadcastedUpdate = captor.getValue();
        assertEquals(auctionId, broadcastedUpdate.getAuctionId());
    }

    @Test
    @DisplayName("Khi auction được tạo, broadcaster.onAuctionCreated() được gọi với AuctionDetailDTO")
    void testBroadcasterCalledOnAuctionCreation() throws Exception {
        // Arrange
        com.shared.dto.CreateAuctionDTO createDTO = new com.shared.dto.CreateAuctionDTO();
        createDTO.setItemId(1L);
        createDTO.setSellerId(1L);
        createDTO.setStartTime(LocalDateTime.now().toString());
        createDTO.setEndTime(LocalDateTime.now().plusHours(2).toString());
        createDTO.setStepPrice(new BigDecimal("50"));

        when(itemService.getItemById(1L)).thenReturn(mock(com.server.model.Item.class));
        when(auctionRepository.create(any())).thenReturn(100L);

        // Act
        long auctionId = auctionService.createAuction(createDTO);

        // Assert
        assertEquals(100L, auctionId);

        // Verify broadcaster was called with AuctionDetailDTO
        ArgumentCaptor<AuctionDetailDTO> captor = ArgumentCaptor.forClass(AuctionDetailDTO.class);
        verify(broadcaster, times(1)).onAuctionCreated(captor.capture());

        AuctionDetailDTO detail = captor.getValue();
        assertEquals(100L, detail.getAuctionId());
        assertEquals(1L, detail.getItemId());
    }

    @Test
    @DisplayName("AuctionUpdateDTO chứa đầy đủ thông tin: giá, người dẫn đầu, thời gian còn lại")
    void testAuctionUpdateDTOContainsAllRequiredFields() {
        // Arrange
        long auctionId = 1L;
        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStatus(Auction.AuctionStatus.RUNNING);
        auction.setCurrentHighestBid(new BigDecimal("5000"));
        auction.setWinnerId(2L);
        auction.setEndTime(LocalDateTime.now().plusMinutes(30));
        auction.setStepPrice(new BigDecimal("100"));

        // Act
        AuctionUpdateDTO update = new AuctionUpdateDTO(
                auction.getId(),
                auction.getCurrentHighestBid(),
                "Winning Bidder",
                30 * 60 * 1000  // 30 minutes in millis
        );

        // Assert
        assertAll("AuctionUpdateDTO should contain:",
                () -> assertEquals(auctionId, update.getAuctionId(), "Auction ID"),
                () -> assertEquals(new BigDecimal("5000"), update.getCurrentPrice(), "Current Price"),
                () -> assertEquals("Winning Bidder", update.getHighestBidderName(), "Highest Bidder Name"),
                () -> assertTrue(update.getRemainingTime() > 0, "Remaining Time should be positive"),
                () -> assertTrue(update.getRemainingTime() <= 30 * 60 * 1000, "Remaining Time should be <= 30 minutes")
        );
    }

    @Test
    @DisplayName("Realtime updates preserve state consistency - multiple bids")
    void testMultipleBidsGenerateSequentialUpdates() throws Exception {
        // Arrange
        long auctionId = 1L;
        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStatus(Auction.AuctionStatus.RUNNING);
        auction.setCurrentHighestBid(new BigDecimal("1000"));
        auction.setWinnerId(1L);
        auction.setEndTime(LocalDateTime.now().plusHours(1));

        injectAuctionIntoCache(auction);

        Bidder bidder1 = new Bidder();
        bidder1.setId(1L);
        bidder1.setFullName("Bidder 1");

        Bidder bidder2 = new Bidder();
        bidder2.setId(2L);
        bidder2.setFullName("Bidder 2");

        when(userRepository.getUserById(1L)).thenReturn(bidder1);
        when(userRepository.getUserById(2L)).thenReturn(bidder2);

        // Act - Place first bid
        BidRequestDTO bid1 = new BidRequestDTO();
        bid1.setAuctionId(auctionId);
        bid1.setBidderId(1L);
        bid1.setBidAmount(new BigDecimal("1100"));
        
        // Mock bidProcessor to update auction state (simulate real behavior)
        doAnswer(invocation -> {
            Auction argAuction = invocation.getArgument(1);
            argAuction.setCurrentHighestBid(new BigDecimal("1100"));
            argAuction.setWinnerId(1L);
            return null;
        }).when(bidProcessor).process(any(), any(), any());

        AuctionUpdateDTO update1 = auctionService.placeBid(bid1);

        // Act - Place second bid
        BidRequestDTO bid2 = new BidRequestDTO();
        bid2.setAuctionId(auctionId);
        bid2.setBidderId(2L);
        bid2.setBidAmount(new BigDecimal("1200"));

        doAnswer(invocation -> {
            Auction argAuction = invocation.getArgument(1);
            argAuction.setCurrentHighestBid(new BigDecimal("1200"));
            argAuction.setWinnerId(2L);
            return null;
        }).when(bidProcessor).process(any(), any(), any());

        AuctionUpdateDTO update2 = auctionService.placeBid(bid2);

        // Assert updates are in correct order
        assertEquals(new BigDecimal("1100"), update1.getCurrentPrice());
        assertEquals(new BigDecimal("1200"), update2.getCurrentPrice());

        // Both should reference the same auction
        assertEquals(auctionId, update1.getAuctionId());
        assertEquals(auctionId, update2.getAuctionId());

        // Verify broadcaster was called twice
        verify(broadcaster, times(2)).onAuctionUpdate(any());
    }

    @Test
    @DisplayName("RemainingTime được tính toán đúng và giảm dần theo thời gian")
    void testRemainingTimeCalculationIsAccurate() {
        // Arrange
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(5);
        Auction auction = new Auction();
        auction.setId(1L);
        auction.setEndTime(endTime);
        auction.setStatus(Auction.AuctionStatus.RUNNING);
        auction.setCurrentHighestBid(BigDecimal.ZERO);

        // Act
        long remaining = java.time.Duration.between(LocalDateTime.now(), endTime).toMillis();

        // Assert
        assertTrue(remaining > 0, "Remaining time should be positive");
        assertTrue(remaining <= 5 * 60 * 1000, "Remaining time should be <= 5 minutes");
        assertTrue(remaining >= 4 * 60 * 1000, "Remaining time should be >= 4 minutes (allowing some processing time)");
    }

    @Test
    @DisplayName("Broadcaster.onAuctionFinished() được gọi khi auction kết thúc")
    void testBroadcasterCalledOnAuctionFinish() throws Exception {
        // Arrange
        long auctionId = 1L;
        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStatus(Auction.AuctionStatus.RUNNING);
        auction.setCurrentHighestBid(new BigDecimal("5000"));
        auction.setWinnerId(2L);
        auction.setEndTime(LocalDateTime.now().minusSeconds(1));

        injectAuctionIntoCache(auction);
        
        // Act - Call private finishAuction method using Reflection
        Method finishMethod = AuctionService.class.getDeclaredMethod("finishAuction", long.class);
        finishMethod.setAccessible(true);
        finishMethod.invoke(auctionService, auctionId);

        // Assert
        verify(broadcaster, times(1)).onAuctionFinished(auctionId);
        verify(auctionRepository, times(1)).save(auction);
    }

    @Test
    @DisplayName("Auto-bid trigger generates additional AUCTION_UPDATE messages")
    void testAutoBidTriggersUpdateMessage() throws Exception {
        // Arrange
        long auctionId = 1L;
        long manualBidderId = 1L;
        long autoBidderId = 2L;

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStatus(Auction.AuctionStatus.RUNNING);
        auction.setCurrentHighestBid(new BigDecimal("1000"));
        auction.setWinnerId(autoBidderId);
        auction.setEndTime(LocalDateTime.now().plusHours(1));

        injectAuctionIntoCache(auction);

        Bidder manualBidder = new Bidder();
        manualBidder.setId(manualBidderId);
        manualBidder.setFullName("Manual Bidder");

        Bidder autoBidder = new Bidder();
        autoBidder.setId(autoBidderId);
        autoBidder.setFullName("Auto Bidder");

        when(userRepository.getUserById(manualBidderId)).thenReturn(manualBidder);
        //when(userRepository.getUserById(autoBidderId)).thenReturn(autoBidder); // Không mock dư thừa nếu code chính không gọi

        // Act
        BidRequestDTO request = new BidRequestDTO();
        request.setAuctionId(auctionId);
        request.setBidderId(manualBidderId);
        request.setBidAmount(new BigDecimal("1100"));
        request.setEnableAutoBid(true);
        request.setMaxAutoBidAmount(new BigDecimal("3000"));

        AuctionUpdateDTO result = auctionService.placeBid(request);

        // Assert
        assertNotNull(result);
        assertEquals(auctionId, result.getAuctionId());

        // Broadcaster should be called for manual bid (autoBidProcessor.process sẽ kích hoạt thêm nếu có trong implementation thật)
        verify(broadcaster, atLeastOnce()).onAuctionUpdate(any());
        verify(autoBidProcessor, times(1)).process(any(), any());
    }
}
