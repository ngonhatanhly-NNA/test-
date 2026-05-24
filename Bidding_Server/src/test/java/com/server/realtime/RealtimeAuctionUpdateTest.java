package com.server.realtime;

import com.server.DAO.*;
import com.server.model.Auction;
import com.server.service.AuctionService;
import com.server.service.ItemService;
import com.server.service.auction.antisnipe.AntiSnipingStrategy;
import com.server.service.auction.core.AuctionCommandService;
import com.server.service.auction.core.AuctionQueryService;
import com.server.service.auction.core.AuctionScheduler;
import com.server.service.auction.core.AuctionSharedState;
import com.server.service.auction.processor.AutoBidProcessor;
import com.server.service.auction.processor.BidProcessor;
import com.server.service.auction.strategy.BidValidationChain;
import com.server.websocket.Broadcaster;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.dto.BidRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("⚡ Realtime Auction Update Tests")
public class RealtimeAuctionUpdateTest {

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
    @Mock private Broadcaster broadcaster;
    @Mock private AuctionScheduler scheduler;
    @Mock private AuctionQueryService queryService;

    private AuctionService auctionService;
    private AuctionSharedState sharedState;
    private AuctionCommandService commandService;

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

        auctionService.setEventListener(broadcaster);
    }

    private void injectAuctionIntoCache(Auction auction) {
        try {
            java.lang.reflect.Field cacheField = AuctionService.class.getDeclaredField("commandService");
            cacheField.setAccessible(true);
            AuctionCommandService cmdService = (AuctionCommandService) cacheField.get(auctionService);

            java.lang.reflect.Field sharedStateField = AuctionCommandService.class.getDeclaredField("sharedState");
            sharedStateField.setAccessible(true);
            AuctionSharedState internalSharedState = (AuctionSharedState) sharedStateField.get(cmdService);

            internalSharedState.auctionCache.put(auction.getId(), auction);
            internalSharedState.auctionLocks.putIfAbsent(auction.getId(), new ReentrantLock());
            
            // Also update our local sharedState reference so we can mock properly if needed
            sharedState.auctionCache.put(auction.getId(), auction);
            sharedState.auctionLocks.putIfAbsent(auction.getId(), new ReentrantLock());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Khi placeBid thành công, broadcaster.onAuctionUpdate() được gọi với dữ liệu đúng")
    void testBroadcasterCalledOnSuccessfulBid() throws Exception {
        // Arrange
        long auctionId = 1L;
        BigDecimal bidAmount = new BigDecimal("1000");

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStatus(Auction.AuctionStatus.RUNNING);
        auction.setCurrentHighestBid(new BigDecimal("900"));
        auction.setWinnerId(1L);
        auction.setEndTime(LocalDateTime.now().plusHours(1));
        auction.setStepPrice(new BigDecimal("10"));

        injectAuctionIntoCache(auction);

        BidRequestDTO bidRequest = new BidRequestDTO(1L, 2L, new BigDecimal("1000"));

        // Use Mockito to answer resolveUserDisplayName directly if possible, but since it's an internal object,
        // we might just let it return the default "User_2" instead of strict mocking.

        // Act
        AuctionUpdateDTO result = auctionService.placeBid(bidRequest);

        // Assert
        assertNotNull(result);
        assertEquals(auctionId, result.getAuctionId());

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
        createDTO.setStartTime(LocalDateTime.now().plusHours(1).toString()); // Scheduled
        createDTO.setEndTime(LocalDateTime.now().plusHours(2).toString());
        createDTO.setStepPrice(new BigDecimal("50"));

        when(itemService.getItemById(1L)).thenReturn(mock(com.server.model.Item.class));
        when(auctionRepository.create(any())).thenReturn(100L);

        // Act
        long auctionId = auctionService.createAuction(createDTO);

        // Assert
        assertEquals(100L, auctionId);
        verify(broadcaster, times(1)).onAuctionCreated(any());
    }
}