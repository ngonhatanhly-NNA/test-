# TODO - Tích Hợp Logging SLF4J/Logback

## Phase 1: Fix Maven Dependencies & Add Logback Config
- [ ] Xóa `slf4j-simple` khỏi `Bidding_Server/pom.xml`
- [ ] Tạo `Bidding_Server/src/main/resources/logback.xml`
- [ ] Tạo `Bidding_Client/src/main/resources/logback.xml`

## Phase 2: Migrate Core Logging Infrastructure
- [ ] Rewrite `AuctionLogger.java` sang SLF4J
- [ ] Update `AdminService.java` (thay System.out còn lại)
- [ ] Add logging `DBConnection.java`

## Phase 3: Migration DAO Layer
- [ ] `UserRepository.java`
- [ ] `AuctionRepository.java`
- [ ] `BidTransactionRepository.java`
- [ ] `ItemRepository.java`
- [ ] `BidderRepository.java`
- [ ] `AutoBidRepository.java`
- [ ] `AdminRepository.java`
- [ ] `SellerRepository.java`

## Phase 4: Controllers & Routes & Security
- [ ] `AuthController.java`
- [ ] `AuctionController.java`
- [ ] `AdminController.java`
- [ ] `TransactionController.java`
- [ ] `ImageController.java`
- [ ] `UserController.java`
- [ ] `ApiRouter.java`
- [ ] `LoginRoute.java`
- [ ] `RegisterRoute.java`
- [ ] `AuthGuard.java`
- [ ] `JwtUtil.java`

## Phase 5: Commands & Models & Misc
- [ ] Command classes in `controller/command/`
- [ ] `Vehicle.java`, `Electronics.java`, `Seller.java`
- [ ] Verify `PaymentProcessor.java` consistency

## Verification
- [ ] `mvn clean compile` pass
- [ ] Run server, check log format

