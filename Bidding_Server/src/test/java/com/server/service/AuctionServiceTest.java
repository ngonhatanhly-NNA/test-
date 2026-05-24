package com.server.service;

import com.server.DAO.*;
import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.server.model.AutoBidTracker;
import com.server.model.Item;
import com.server.service.auction.antisnipe.AntiSnipingStrategy;
import com.server.service.auction.core.AuctionCommandService;
import com.server.service.auction.core.AuctionQueryService;
import com.server.service.auction.core.AuctionScheduler;
import com.server.service.auction.core.AuctionSharedState;
import com.server.service.auction.processor.AutoBidProcessor;
import com.server.service.auction.processor.BidProcessor;
import com.server.service.auction.strategy.BidValidationChain;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.dto.BidRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionService - Quản lý Đấu giá")
class AuctionServiceTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private BidTransactionRepository bidRepository;
    @Mock private AutoBidRepository autoBidRepository;
    @Mock private ItemService itemService;
    @Mock private UserRepository userRepository;
    @Mock private IBidderRepository bidderRepository;
    @Mock private BidValidationChain validationChain;
    @Mock private AntiSnipingStrategy antiSnipingStrategy;
    @Mock private BidProcessor bidProcessor;
    @Mock private AutoBidProcessor autoBidProcessor;
    @Mock private AuctionScheduler scheduler;
    @Mock private AuctionQueryService queryService;

    private AuctionService auctionService;
    private AuctionSharedState sharedState;
    private AuctionCommandService commandService;
    private Auction sampleAuction;
    private BidRequestDTO sampleBidRequest;

    @BeforeEach
    void setUp() {
        sharedState = new AuctionSharedState(userRepository);
        commandService = new AuctionCommandService(
                sharedState,
                auctionRepository,
                autoBidRepository,
                bidRepository,
                itemService,
                validationChain,
                antiSnipingStrategy,
                bidProcessor,
                autoBidProcessor,
                scheduler,
                queryService
        );
        auctionService = new AuctionService(
                auctionRepository,
                bidRepository,
                autoBidRepository,
                itemService,
                userRepository,
                bidderRepository,
                validationChain,
                antiSnipingStrategy,
                bidProcessor,
                autoBidProcessor
        );

        LocalDateTime now = LocalDateTime.now();
        sampleAuction = new Auction();
        sampleAuction.setId(1L);
        sampleAuction.setItemId(101L);
        sampleAuction.setSellerId(1L);
        sampleAuction.setStartTime(now.minusHours(1));
        sampleAuction.setEndTime(now.plusHours(1));
        sampleAuction.setStepPrice(new BigDecimal("10"));
        sampleAuction.setCurrentHighestBid(new BigDecimal("100"));
        sampleAuction.setWinnerId(2L);
        sampleAuction.setStatus(Auction.AuctionStatus.RUNNING);

        sampleBidRequest = new BidRequestDTO(1L, 3L, new BigDecimal("120"), false, null);

        sharedState.auctionCache.put(sampleAuction.getId(), sampleAuction);
        sharedState.auctionLocks.putIfAbsent(sampleAuction.getId(), new ReentrantLock());
    }

    @Nested
    @DisplayName("Logic Đặt giá (placeBid)")
    class PlaceBidTests {

        @Test
        @DisplayName("Nên ném lỗi khi validation thất bại")
        void placeBid_shouldThrowException_whenValidationFails() throws AuctionException {
            doThrow(new AuctionException(AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW))
                    .when(validationChain).validate(any(), any());

            AuctionException exception = assertThrows(AuctionException.class,
                    () -> commandService.placeBid(sampleBidRequest));
            assertEquals(AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW, exception.getErrorCode());
            verify(bidProcessor, never()).process(any(), any(), any());
        }

        @Test
        @DisplayName("Nên xử lý thành công một bid thủ công hợp lệ")
        void placeBid_shouldSucceed_forValidManualBid() throws AuctionException {
            doNothing().when(validationChain).validate(any(), any());
            doNothing().when(bidProcessor).process(any(), any(), any());
            when(antiSnipingStrategy.shouldExtendTime(any())).thenReturn(false);

            AuctionUpdateDTO result = commandService.placeBid(sampleBidRequest);

            assertNotNull(result);
            assertEquals(1L, result.getAuctionId());
            verify(validationChain, times(1)).validate(eq(sampleBidRequest), any(Auction.class));
            verify(bidProcessor, times(1)).process(any(), any(), any());
        }

        @Test
        @DisplayName("Nên kích hoạt Anti-Sniping khi đặt giá ở giây cuối")
        void placeBid_shouldTriggerAntiSniping_forLastSecondBid() throws AuctionException {
            doNothing().when(validationChain).validate(any(), any());
            doNothing().when(bidProcessor).process(any(), any(), any());
            when(antiSnipingStrategy.shouldExtendTime(any())).thenReturn(true);
            when(antiSnipingStrategy.getExtensionMillis()).thenReturn(30000L);

            LocalDateTime originalEndTime = sampleAuction.getEndTime();
            commandService.placeBid(sampleBidRequest);

            LocalDateTime newEndTime = sampleAuction.getEndTime();
            assertTrue(newEndTime.isAfter(originalEndTime));
            verify(antiSnipingStrategy, times(1)).shouldExtendTime(any(Auction.class));
        }

        @Test
        @DisplayName("Nên đăng ký Auto-Bid khi được yêu cầu")
        void placeBid_shouldRegisterAutoBid_whenRequested() throws AuctionException {
            sampleBidRequest.setEnableAutoBid(true);
            sampleBidRequest.setMaxAutoBidAmount(new BigDecimal("500"));
            doNothing().when(validationChain).validate(any(), any());
            doNothing().when(bidProcessor).process(any(), any(), any());

            commandService.placeBid(sampleBidRequest);

            ArgumentCaptor<AutoBidTracker> captor = ArgumentCaptor.forClass(AutoBidTracker.class);
            verify(autoBidRepository, times(1)).saveOrUpdate(captor.capture());

            AutoBidTracker savedAutoBid = captor.getValue();
            assertEquals(1L, savedAutoBid.getAuctionId());
            assertEquals(3L, savedAutoBid.getBidderId());
        }
    }
}