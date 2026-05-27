package com.server.service;

import com.server.DAO.*;
import com.server.exception.AuctionException;
import com.server.model.Auction;
import com.server.model.AutoBidTracker;
import com.server.model.Electronics;
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
import com.shared.dto.CreateAuctionDTO;
import org.junit.jupiter.api.AfterEach;
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
    private AuctionCommandService commandService;
    private AuctionSharedState sharedState;
    private BidRequestDTO sampleBidRequest;

    @BeforeEach
    void setUp() throws Exception {
        sharedState = new AuctionSharedState(userRepository);
        commandService = new AuctionCommandService(
                sharedState, auctionRepository, autoBidRepository, bidRepository,
                itemService, validationChain, antiSnipingStrategy, bidProcessor,
                autoBidProcessor, scheduler, queryService
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

        // Stop the real services created by AuctionService to avoid thread leaks
        auctionService.shutdown();

        // Inject our customized instances into auctionService via reflection
        Field cmdField = AuctionService.class.getDeclaredField("commandService");
        cmdField.setAccessible(true);
        cmdField.set(auctionService, commandService);

        Field qryField = AuctionService.class.getDeclaredField("queryService");
        qryField.setAccessible(true);
        qryField.set(auctionService, queryService);

        Field schField = AuctionService.class.getDeclaredField("scheduler");
        schField.setAccessible(true);
        schField.set(auctionService, scheduler);

        LocalDateTime now = LocalDateTime.now();
        Auction sampleAuction = new Auction();
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

    @AfterEach
    void tearDown() {
        if (commandService != null) {
            commandService.shutdown();
        }
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
                    () -> auctionService.placeBid(sampleBidRequest));
            assertEquals(AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW, exception.getErrorCode());
            verify(bidProcessor, never()).process(any(), any(), any());
        }

        @Test
        @DisplayName("Nên xử lý thành công một bid thủ công hợp lệ")
        void placeBid_shouldSucceed_forValidManualBid() throws AuctionException {
            doNothing().when(validationChain).validate(any(), any());
            doNothing().when(bidProcessor).process(any(), any(), any());
            when(antiSnipingStrategy.shouldExtendTime(any())).thenReturn(false);

            AuctionUpdateDTO result = auctionService.placeBid(sampleBidRequest);

            assertNotNull(result);
            assertEquals(1L, result.getAuctionId());
            verify(validationChain, times(1)).validate(eq(sampleBidRequest), any(Auction.class));
            verify(bidProcessor, times(1)).process(any(), any(), any());
        }

        @Test
        @DisplayName("Nên ném lỗi khi request null")
        void placeBid_shouldThrowException_whenRequestNull() {
            AuctionException exception = assertThrows(AuctionException.class, () -> auctionService.placeBid(null));
            assertEquals(AuctionException.ErrorCode.INVALID_BID_AMOUNT, exception.getErrorCode());
        }

        @Test
        @DisplayName("Nên ném lỗi khi phiên đấu giá không tồn tại")
        void placeBid_shouldThrowException_whenAuctionNotFound() {
            BidRequestDTO invalidRequest = new BidRequestDTO(999L, 3L, new BigDecimal("120"), false, null);
            AuctionException exception = assertThrows(AuctionException.class, () -> auctionService.placeBid(invalidRequest));
            assertEquals(AuctionException.ErrorCode.AUCTION_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Logic Tạo Đấu giá (createAuction)")
    class CreateAuctionTests {

        @Test
        @DisplayName("Nên tạo auction thành công với dữ liệu hợp lệ")
        void createAuction_shouldSucceed_withValidData() throws AuctionException {
            CreateAuctionDTO dto = new CreateAuctionDTO();
            dto.setItemId(1L);
            dto.setSellerId(1L);
            dto.setStartTime(LocalDateTime.now().toString());
            dto.setEndTime(LocalDateTime.now().plusHours(1).toString());
            dto.setStepPrice(new BigDecimal("10"));

            when(itemService.getItemById(1L)).thenReturn(new Electronics.Builder().build());
            when(auctionRepository.create(any(Auction.class))).thenReturn(100L);

            long auctionId = auctionService.createAuction(dto);

            assertEquals(100L, auctionId);
            verify(scheduler, times(1)).cacheAndScheduleAuction(any(Auction.class));
        }

        @Test
        @DisplayName("Nên ném lỗi khi Item không tồn tại")
        void createAuction_shouldThrowException_whenItemNotFound() {
            CreateAuctionDTO dto = new CreateAuctionDTO();
            dto.setItemId(999L);
            dto.setSellerId(1L);
            dto.setStartTime(LocalDateTime.now().toString());
            dto.setEndTime(LocalDateTime.now().plusHours(1).toString());
            dto.setStepPrice(new BigDecimal("10"));

            when(itemService.getItemById(999L)).thenReturn(null);

            assertThrows(AuctionException.class, () -> auctionService.createAuction(dto));
        }

        @Test
        @DisplayName("Nên ném lỗi khi ItemId hoặc SellerId <= 0")
        void createAuction_shouldThrowException_whenIdsInvalid() {
            CreateAuctionDTO dto = new CreateAuctionDTO();
            dto.setItemId(0L);
            dto.setSellerId(1L);
            assertThrows(AuctionException.class, () -> auctionService.createAuction(dto));

            dto.setItemId(1L);
            dto.setSellerId(0L);
            assertThrows(AuctionException.class, () -> auctionService.createAuction(dto));
        }

        @Test
        @DisplayName("Nên ném lỗi khi StepPrice không hợp lệ")
        void createAuction_shouldThrowException_whenStepPriceInvalid() {
            CreateAuctionDTO dto = new CreateAuctionDTO();
            dto.setItemId(1L);
            dto.setSellerId(1L);
            dto.setStepPrice(BigDecimal.ZERO);
            assertThrows(AuctionException.class, () -> auctionService.createAuction(dto));

            dto.setStepPrice(null);
            assertThrows(AuctionException.class, () -> auctionService.createAuction(dto));
        }
    }

    @Nested
    @DisplayName("Logic Auto-Bid")
    class AutoBidTests {

        @Test
        @DisplayName("Nên hủy auto-bid thành công")
        void cancelAutoBid_shouldSucceed() throws AuctionException {
            auctionService.cancelAutoBid(1L, 1L);
            verify(autoBidRepository, times(1)).deactivate(1L, 1L);
        }

        @Test
        @DisplayName("Nên cập nhật max amount cho auto-bid")
        void updateAutoBidAmount_shouldSucceed() throws AuctionException {
            auctionService.updateAutoBidAmount(1L, 1L, new BigDecimal("1000"), null);
            ArgumentCaptor<AutoBidTracker> captor = ArgumentCaptor.forClass(AutoBidTracker.class);
            verify(autoBidRepository, times(1)).saveOrUpdate(captor.capture());
            assertEquals(new BigDecimal("1000"), captor.getValue().getMaxBidAmount());
        }

        @Test
        @DisplayName("Nên ném lỗi khi update auto-bid với số tiền không hợp lệ")
        void updateAutoBidAmount_shouldThrowException_whenAmountInvalid() {
            assertThrows(AuctionException.class, () -> auctionService.updateAutoBidAmount(1L, 1L, BigDecimal.ZERO, null));
            assertThrows(AuctionException.class, () -> auctionService.updateAutoBidAmount(1L, 1L, null, null));
        }
    }

    @Nested
    @DisplayName("Logic Query")
    class QueryTests {
        @Test
        @DisplayName("Nên trả về chi tiết auction khi gọi getAuctionDetail")
        void getAuctionDetail_shouldReturnDetail() throws AuctionException {
            when(queryService.getAuctionDetail(1L)).thenReturn(new AuctionDetailDTO());
            AuctionDetailDTO result = auctionService.getAuctionDetail(1L);
            assertNotNull(result);
            verify(queryService, times(1)).getAuctionDetail(1L);
        }

        @Test
        @DisplayName("Các phương thức lấy danh sách nên gọi đúng query service")
        void queryListMethods_shouldCallQueryService() {
            auctionService.getBidHistory(1L);
            verify(queryService, times(1)).getBidHistory(1L);

            auctionService.getActiveAuctions();
            verify(queryService, times(1)).getActiveAuctions();

            auctionService.getUpcomingAuctions();
            verify(queryService, times(1)).getUpcomingAuctions();

            auctionService.getActiveAuctionsBySeller(1L);
            verify(queryService, times(1)).getActiveAuctionsBySeller(1L);

            auctionService.getWonAuctionsByBidder(1L);
            verify(queryService, times(1)).getWonAuctionsByBidder(1L);

            auctionService.getAuctionStatusByItemId(1L);
            verify(queryService, times(1)).getAuctionStatusByItemId(1L);
        }
    }
}