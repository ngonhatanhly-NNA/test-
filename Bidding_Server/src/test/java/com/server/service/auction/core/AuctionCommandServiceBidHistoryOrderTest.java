package com.server.service.auction.core;

import com.server.DAO.AuctionRepository;
import com.server.DAO.AutoBidRepository;
import com.server.DAO.BidTransactionRepository;
import com.server.DAO.UserRepository;
import com.server.model.Auction;
import com.server.model.BidTransaction;
import com.server.service.ItemService;
import com.server.service.auction.antisnipe.AntiSnipingStrategy;
import com.server.service.auction.processor.AutoBidProcessor;
import com.server.service.auction.processor.BidProcessor;
import com.server.service.auction.strategy.BidValidationChain;
import com.shared.dto.BidRequestDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AuctionCommandServiceBidHistoryOrderTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private AutoBidRepository autoBidRepository;
    @Mock
    private BidTransactionRepository bidRepository;
    @Mock
    private ItemService itemService;
    @Mock
    private BidValidationChain validationChain;
    @Mock
    private AntiSnipingStrategy antiSnipingStrategy;
    @Mock
    private BidProcessor bidProcessor;
    @Mock
    private AutoBidProcessor autoBidProcessor;
    @Mock
    private AuctionScheduler scheduler;
    @Mock
    private AuctionQueryService queryService;
    @Mock
    private UserRepository userRepository;

    private AuctionCommandService commandService;
    private AuctionSharedState sharedState;

    @BeforeEach
    void setUp() {
        sharedState = new AuctionSharedState(userRepository);
        Auction auction = new Auction();
        auction.setId(1L);
        auction.setStatus(Auction.AuctionStatus.RUNNING);
        auction.setCurrentHighestBid(new BigDecimal("100"));
        auction.setStepPrice(new BigDecimal("10"));
        auction.setEndTime(LocalDateTime.now().plusMinutes(5));
        sharedState.auctionCache.put(1L, auction);
        sharedState.auctionLocks.put(1L, new ReentrantLock());

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
    }

    @AfterEach
    void tearDown() {
        commandService.shutdown();
    }

    @Test
    @DisplayName("Persist manual then auto bids in deterministic sequence")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void placeBid_shouldPersistManualThenAutoInOrder() throws Exception {
        commandService.shutdown();

        BidRequestDTO request = new BidRequestDTO(1L, 11L, new BigDecimal("120"), false, null);

        doNothing().when(validationChain).validate(any(), any());
        when(antiSnipingStrategy.shouldExtendTime(any())).thenReturn(false);
        when(bidRepository.findMaxEventSequenceByAuction(1L)).thenReturn(0L);

        doAnswer(invocation -> {
            List<BidTransaction> bidBatch = invocation.getArgument(2);
            BidTransaction manual = new BidTransaction(1L, 11L, new BigDecimal("120"));
            manual.setAutoBid(false);
            bidBatch.add(manual);
            return null;
        }).when(bidProcessor).process(any(), any(), any());

        doAnswer(invocation -> {
            List<BidTransaction> bidBatch = invocation.getArgument(1);
            BidTransaction auto = new BidTransaction(1L, 22L, new BigDecimal("130"));
            auto.setAutoBid(true);
            bidBatch.add(auto);
            return null;
        }).when(autoBidProcessor).process(any(), any());

        commandService.placeBid(request);

        BidPersistenceBatch manualBatch = sharedState.bidQueue.take();
        BidPersistenceBatch autoBatch = sharedState.bidQueue.take();

        Method persistMethod = AuctionCommandService.class.getDeclaredMethod("persistBidBatch", BidPersistenceBatch.class);
        persistMethod.setAccessible(true);
        persistMethod.invoke(commandService, manualBatch);
        persistMethod.invoke(commandService, autoBatch);

        ArgumentCaptor<List<BidTransaction>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(bidRepository, times(2)).saveBatchTransactional(captor.capture());

        List<List<BidTransaction>> allBatches = captor.getAllValues();
        List<BidTransaction> persisted = new ArrayList<>();
        allBatches.forEach(persisted::addAll);

        assertEquals(2, persisted.size());
        assertFalse(persisted.get(0).isAutoBid());
        assertTrue(persisted.get(1).isAutoBid());
        assertEquals(1L, persisted.get(0).getEventSequence());
        assertEquals(2L, persisted.get(1).getEventSequence());
    }
}