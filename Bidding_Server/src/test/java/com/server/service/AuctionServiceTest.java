package com.server.service;

import com.server.DAO.*;
import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.server.model.AutoBidTracker; // SỬA LỖI: Import lớp còn thiếu
import com.server.service.auction.antisnipe.AntiSnipingStrategy;
import com.server.service.auction.processor.AutoBidProcessor;
import com.server.service.auction.processor.BidProcessor;
import com.server.service.auction.strategy.BidValidationChain;
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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử Dịch vụ Đấu giá (AuctionService)")
class AuctionServiceTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private BidTransactionRepository bidRepository;
    @Mock private AutoBidRepository autoBidRepository;
    @Mock private BidValidationChain validationChain;
    @Mock private AntiSnipingStrategy antiSnipingStrategy;
    @Mock private BidProcessor bidProcessor;
    @Mock private AutoBidProcessor autoBidProcessor;

    private AuctionService auctionService;

    private Auction sampleAuction;
    private BidRequestDTO sampleBidRequest;

    @BeforeEach
    void setUp() throws Exception {
        auctionService = new AuctionService(
                auctionRepository,
                bidRepository,
                autoBidRepository,
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

        // SỬA LỖI: Không gọi init() private. Thay vào đó, mô phỏng hành vi của nó.
        // Dùng Reflection để truy cập và điền vào cache.
        Field cacheField = AuctionService.class.getDeclaredField("auctionCache");
        cacheField.setAccessible(true);
        ConcurrentHashMap<Long, Auction> auctionCache = (ConcurrentHashMap<Long, Auction>) cacheField.get(auctionService);
        auctionCache.put(sampleAuction.getId(), sampleAuction);

        Field lockField = AuctionService.class.getDeclaredField("auctionLocks");
        lockField.setAccessible(true);
        ConcurrentHashMap<Long, ReentrantLock> auctionLocks = (ConcurrentHashMap<Long, ReentrantLock>) lockField.get(auctionService);
        auctionLocks.put(sampleAuction.getId(), new ReentrantLock());
    }

    @Nested
    @DisplayName("Kiểm thử Logic Đặt giá (placeBid)")
    class PlaceBidTests {

        @Test
        @DisplayName("Nên ném lỗi khi validation thất bại")
        void placeBid_shouldThrowException_whenValidationFails() throws AuctionException {
            doThrow(new AuctionException(AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW))
                    .when(validationChain).validate(any(), any());

            AuctionException exception = assertThrows(AuctionException.class,
                    () -> auctionService.placeBid(sampleBidRequest));

            assertEquals(AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW, exception.getErrorCode());
            verify(bidProcessor, never()).process(any(), any(), any());
            verify(autoBidProcessor, never()).process(any(), any());
        }

        @Test
        @DisplayName("Nên xử lý thành công một bid thủ công hợp lệ")
        void placeBid_shouldSucceed_forValidManualBid() throws AuctionException {
            doNothing().when(validationChain).validate(any(), any());
            doNothing().when(bidProcessor).process(any(), any(), any());
            when(antiSnipingStrategy.shouldExtendTime(any())).thenReturn(false);
            doNothing().when(autoBidProcessor).process(any(), any());

            AuctionUpdateDTO result = auctionService.placeBid(sampleBidRequest);

            assertNotNull(result);
            assertEquals(1L, result.getAuctionId());
            // SỬA LỖI: Dùng đúng tên getter từ DTO
            assertEquals(0, new BigDecimal("120").compareTo(result.getCurrentPrice()));
            assertEquals("User_3", result.getHighestBidderName());

            verify(validationChain, times(1)).validate(eq(sampleBidRequest), any(Auction.class));
            verify(bidProcessor, times(1)).process(eq(sampleBidRequest), any(Auction.class), any(LinkedBlockingQueue.class));
            verify(autoBidProcessor, times(1)).process(any(Auction.class), any(LinkedBlockingQueue.class));
        }

        @Test
        @DisplayName("Nên kích hoạt Anti-Sniping khi đặt giá ở giây cuối")
        void placeBid_shouldTriggerAntiSniping_forLastSecondBid() throws AuctionException {
            doNothing().when(validationChain).validate(any(), any());
            doNothing().when(bidProcessor).process(any(), any(), any());
            when(antiSnipingStrategy.shouldExtendTime(any())).thenReturn(true);
            when(antiSnipingStrategy.getExtensionMillis()).thenReturn(30000L);

            LocalDateTime originalEndTime = sampleAuction.getEndTime();

            auctionService.placeBid(sampleBidRequest);

            LocalDateTime newEndTime = sampleAuction.getEndTime();
            assertTrue(newEndTime.isAfter(originalEndTime));
            assertEquals(originalEndTime.plusSeconds(30), newEndTime);

            verify(antiSnipingStrategy, times(1)).shouldExtendTime(any(Auction.class));
        }

        @Test
        @DisplayName("Nên đăng ký Auto-Bid khi được yêu cầu")
        void placeBid_shouldRegisterAutoBid_whenRequested() throws AuctionException {
            sampleBidRequest.setEnableAutoBid(true);
            sampleBidRequest.setMaxAutoBidAmount(new BigDecimal("500"));
            doNothing().when(validationChain).validate(any(), any());

            auctionService.placeBid(sampleBidRequest);

            ArgumentCaptor<AutoBidTracker> captor = ArgumentCaptor.forClass(AutoBidTracker.class);
            // SỬA LỖI: Đảm bảo verify đúng phương thức
            verify(autoBidRepository, times(1)).saveOrUpdate(captor.capture());

            AutoBidTracker savedAutoBid = captor.getValue();
            assertEquals(1L, savedAutoBid.getAuctionId());
            assertEquals(3L, savedAutoBid.getBidderId());
            assertEquals(0, new BigDecimal("500").compareTo(savedAutoBid.getMaxBidAmount()));
        }
    }

    @Nested
    @DisplayName("Kiểm thử Vòng đời Phiên đấu giá")
    class AuctionLifecycleTests {

        @Test
        @DisplayName("Nên lấy chi tiết phiên đấu giá thành công từ cache")
        void getAuctionDetail_shouldReturnDetails_fromCache() throws AuctionException {
            when(auctionRepository.findItemNameByItemId(101L)).thenReturn("Test Item");

            var detail = auctionService.getAuctionDetail(1L);

            assertNotNull(detail);
            assertEquals(1L, detail.getAuctionId());
            assertEquals("Test Item", detail.getItemName());
            verify(auctionRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Nên lấy chi tiết phiên đấu giá thành công từ DB khi không có trong cache")
        void getAuctionDetail_shouldReturnDetails_fromDbWhenNotInCache() throws AuctionException {
            long auctionIdNotInCache = 99L;
            Auction dbAuction = new Auction();
            dbAuction.setId(auctionIdNotInCache);
            dbAuction.setEndTime(LocalDateTime.now().plusHours(1));
            when(auctionRepository.findById(auctionIdNotInCache)).thenReturn(Optional.of(dbAuction));
            when(auctionRepository.findItemNameByItemId(anyLong())).thenReturn("DB Item");

            var detail = auctionService.getAuctionDetail(auctionIdNotInCache);

            assertNotNull(detail);
            assertEquals(auctionIdNotInCache, detail.getAuctionId());
            assertEquals("DB Item", detail.getItemName());
            verify(auctionRepository, times(1)).findById(auctionIdNotInCache);
        }
    }
}