package com.server.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho toàn bộ exception package.
 * Mục tiêu: đảm bảo JaCoCo coverage >= 80% cho package exception.
 */
@DisplayName("Exception Package Tests")
class ExceptionTest {

    // =========================================================================
    // AppException
    // =========================================================================
    @Nested
    @DisplayName("AppException")
    class AppExceptionTests {

        @Test
        @DisplayName("Constructor 2 tham số - default status 400")
        void testTwoArgConstructor() {
            AppException ex = new AppException("ERR_001", "Something went wrong");
            assertEquals("ERR_001", ex.getErrorCode());
            assertEquals("Something went wrong", ex.getMessage());
            assertEquals(400, ex.getHttpStatus());
        }

        @Test
        @DisplayName("Constructor 3 tham số - custom status")
        void testThreeArgConstructor() {
            AppException ex = new AppException("ERR_002", "Not found", 404);
            assertEquals("ERR_002", ex.getErrorCode());
            assertEquals("Not found", ex.getMessage());
            assertEquals(404, ex.getHttpStatus());
        }

        @Test
        @DisplayName("AppException là RuntimeException")
        void testIsRuntimeException() {
            AppException ex = new AppException("CODE", "msg");
            assertInstanceOf(RuntimeException.class, ex);
        }
    }

    // =========================================================================
    // AuctionException
    // =========================================================================
    @Nested
    @DisplayName("AuctionException")
    class AuctionExceptionTests {

        @Test
        @DisplayName("Constructor với ErrorCode")
        void testConstructorWithErrorCode() {
            AuctionException ex = new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND);
            assertEquals(AuctionException.ErrorCode.AUCTION_NOT_FOUND, ex.getErrorCode());
            assertEquals(AuctionException.ErrorCode.AUCTION_NOT_FOUND.getMessage(), ex.getMessage());
        }

        @Test
        @DisplayName("Constructor với ErrorCode và Throwable cause")
        void testConstructorWithCause() {
            Throwable cause = new RuntimeException("db error");
            AuctionException ex = new AuctionException(AuctionException.ErrorCode.OPERATION_FAILED, cause);
            assertEquals(AuctionException.ErrorCode.OPERATION_FAILED, ex.getErrorCode());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("Constructor với ErrorCode và detail string")
        void testConstructorWithDetail() {
            AuctionException ex = new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND, "id=42");
            assertTrue(ex.getMessage().contains("id=42"));
            assertEquals(AuctionException.ErrorCode.AUCTION_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("AuctionException là checked Exception")
        void testIsCheckedException() {
            AuctionException ex = new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_ACTIVE);
            assertInstanceOf(Exception.class, ex);
            // AuctionException extends Exception (checked), không phải RuntimeException
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("Tất cả ErrorCode có message không rỗng")
        void testAllErrorCodesHaveMessages() {
            for (AuctionException.ErrorCode code : AuctionException.ErrorCode.values()) {
                assertNotNull(code.getMessage(), "ErrorCode " + code.name() + " phải có message");
                assertFalse(code.getMessage().isBlank(), "ErrorCode " + code.name() + " message không được rỗng");
            }
        }

        @Test
        @DisplayName("Kiểm tra một số ErrorCode cụ thể")
        void testSpecificErrorCodes() {
            assertEquals(AuctionException.ErrorCode.AUCTION_NOT_FOUND,
                    new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND).getErrorCode());
            assertEquals(AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW,
                    new AuctionException(AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW).getErrorCode());
            assertEquals(AuctionException.ErrorCode.INSUFFICIENT_BALANCE,
                    new AuctionException(AuctionException.ErrorCode.INSUFFICIENT_BALANCE).getErrorCode());
            assertEquals(AuctionException.ErrorCode.SELF_BID_NOT_ALLOWED,
                    new AuctionException(AuctionException.ErrorCode.SELF_BID_NOT_ALLOWED).getErrorCode());
        }
    }

    // =========================================================================
    // AuctionAppException
    // =========================================================================
    @Nested
    @DisplayName("AuctionAppException")
    class AuctionAppExceptionTests {

        @Test
        @DisplayName("Constructor 3 tham số")
        void testConstructor() {
            AuctionAppException ex = new AuctionAppException("AUCTION_NOT_FOUND", "Not found", 404);
            assertEquals("AUCTION_NOT_FOUND", ex.getErrorCode());
            assertEquals("Not found", ex.getMessage());
            assertEquals(404, ex.getHttpStatus());
        }

        @Test
        @DisplayName("from() - AUCTION_NOT_FOUND -> 404")
        void testFromAuctionNotFound() {
            AuctionException source = new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND);
            AuctionAppException ex = AuctionAppException.from(source);
            assertEquals(404, ex.getHttpStatus());
        }

        @Test
        @DisplayName("from() - AUCTION_NOT_ACTIVE -> 404")
        void testFromAuctionNotActive() {
            AuctionAppException ex = AuctionAppException.from(
                    new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_ACTIVE));
            assertEquals(404, ex.getHttpStatus());
        }

        @Test
        @DisplayName("from() - AUTO_BID_NOT_FOUND -> 404")
        void testFromAutoBidNotFound() {
            AuctionAppException ex = AuctionAppException.from(
                    new AuctionException(AuctionException.ErrorCode.AUTO_BID_NOT_FOUND));
            assertEquals(404, ex.getHttpStatus());
        }

        @Test
        @DisplayName("from() - INVALID_REQUEST -> 400")
        void testFromInvalidRequest() {
            AuctionAppException ex = AuctionAppException.from(
                    new AuctionException(AuctionException.ErrorCode.INVALID_REQUEST));
            assertEquals(400, ex.getHttpStatus());
        }

        @Test
        @DisplayName("from() - INVALID_BID_AMOUNT -> 400")
        void testFromInvalidBidAmount() {
            AuctionAppException ex = AuctionAppException.from(
                    new AuctionException(AuctionException.ErrorCode.INVALID_BID_AMOUNT));
            assertEquals(400, ex.getHttpStatus());
        }

        @Test
        @DisplayName("from() - BID_AMOUNT_TOO_LOW -> 400")
        void testFromBidAmountTooLow() {
            AuctionAppException ex = AuctionAppException.from(
                    new AuctionException(AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW));
            assertEquals(400, ex.getHttpStatus());
        }

        @Test
        @DisplayName("from() - INVALID_AUTO_BID_CONFIG -> 400")
        void testFromInvalidAutoBidConfig() {
            AuctionAppException ex = AuctionAppException.from(
                    new AuctionException(AuctionException.ErrorCode.INVALID_AUTO_BID_CONFIG));
            assertEquals(400, ex.getHttpStatus());
        }

        @Test
        @DisplayName("from() - AUCTION_ALREADY_FINISHED -> 410")
        void testFromAuctionAlreadyFinished() {
            AuctionAppException ex = AuctionAppException.from(
                    new AuctionException(AuctionException.ErrorCode.AUCTION_ALREADY_FINISHED));
            assertEquals(410, ex.getHttpStatus());
        }

        @Test
        @DisplayName("from() - OPERATION_FAILED -> 500")
        void testFromOperationFailed() {
            AuctionAppException ex = AuctionAppException.from(
                    new AuctionException(AuctionException.ErrorCode.OPERATION_FAILED));
            assertEquals(500, ex.getHttpStatus());
        }

        @Test
        @DisplayName("from() - USER_NOT_FOUND -> default 500")
        void testFromUserNotFound() {
            AuctionAppException ex = AuctionAppException.from(
                    new AuctionException(AuctionException.ErrorCode.USER_NOT_FOUND));
            assertEquals(500, ex.getHttpStatus());
        }

        @Test
        @DisplayName("from() - INSUFFICIENT_BALANCE -> default 500")
        void testFromInsufficientBalance() {
            AuctionAppException ex = AuctionAppException.from(
                    new AuctionException(AuctionException.ErrorCode.INSUFFICIENT_BALANCE));
            assertEquals(500, ex.getHttpStatus());
        }

        @Test
        @DisplayName("from() - SELF_BID_NOT_ALLOWED -> default 500")
        void testFromSelfBidNotAllowed() {
            AuctionAppException ex = AuctionAppException.from(
                    new AuctionException(AuctionException.ErrorCode.SELF_BID_NOT_ALLOWED));
            assertEquals(500, ex.getHttpStatus());
        }

        @Test
        @DisplayName("from() giữ nguyên error code name")
        void testFromPreservesErrorCodeName() {
            AuctionException source = new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND);
            AuctionAppException ex = AuctionAppException.from(source);
            assertEquals("AUCTION_NOT_FOUND", ex.getErrorCode());
        }

        @Test
        @DisplayName("AuctionAppException kế thừa AppException")
        void testInheritance() {
            AuctionAppException ex = new AuctionAppException("CODE", "msg", 400);
            assertInstanceOf(AppException.class, ex);
        }
    }

    // =========================================================================
    // AuthValidationException
    // =========================================================================
    @Nested
    @DisplayName("AuthValidationException")
    class AuthValidationExceptionTests {

        @Test
        @DisplayName("Constructor với ErrorCode enum")
        void testEnumConstructor() {
            AuthValidationException ex = new AuthValidationException(
                    AuthValidationException.ErrorCode.USERNAME_BLANK);
            assertEquals("USERNAME_BLANK", ex.getErrorCode());
            assertEquals(400, ex.getHttpStatus());
            assertEquals(AuthValidationException.ErrorCode.USERNAME_BLANK.getMessage(), ex.getMessage());
        }

        @Test
        @DisplayName("Constructor với String message (legacy)")
        void testStringConstructor() {
            AuthValidationException ex = new AuthValidationException("Custom error message");
            assertEquals("VALIDATION_ERROR", ex.getErrorCode());
            assertEquals(400, ex.getHttpStatus());
            assertEquals("Custom error message", ex.getMessage());
        }

        @Test
        @DisplayName("Tất cả ErrorCode hoạt động đúng")
        void testAllErrorCodes() {
            for (AuthValidationException.ErrorCode code : AuthValidationException.ErrorCode.values()) {
                AuthValidationException ex = new AuthValidationException(code);
                assertEquals(code.name(), ex.getErrorCode());
                assertEquals(400, ex.getHttpStatus());
                assertNotNull(code.getMessage());
            }
        }

        @Test
        @DisplayName("Kiểm tra từng ErrorCode cụ thể")
        void testSpecificErrorCodes() {
            assertEquals("INVALID_REQUEST_DATA",
                    new AuthValidationException(AuthValidationException.ErrorCode.INVALID_REQUEST_DATA).getErrorCode());
            assertEquals("PASSWORD_BLANK",
                    new AuthValidationException(AuthValidationException.ErrorCode.PASSWORD_BLANK).getErrorCode());
            assertEquals("EMAIL_BLANK",
                    new AuthValidationException(AuthValidationException.ErrorCode.EMAIL_BLANK).getErrorCode());
            assertEquals("FULLNAME_BLANK",
                    new AuthValidationException(AuthValidationException.ErrorCode.FULLNAME_BLANK).getErrorCode());
        }

        @Test
        @DisplayName("AuthValidationException kế thừa AppException")
        void testInheritance() {
            AuthValidationException ex = new AuthValidationException("test");
            assertInstanceOf(AppException.class, ex);
        }
    }

    // =========================================================================
    // DuplicateUserException
    // =========================================================================
    @Nested
    @DisplayName("DuplicateUserException")
    class DuplicateUserExceptionTests {

        @Test
        @DisplayName("USERNAME_EXISTED với detail")
        void testUsernameExisted() {
            DuplicateUserException ex = new DuplicateUserException(
                    DuplicateUserException.ErrorCode.USERNAME_EXISTED, "testuser");
            assertEquals("USERNAME_EXISTED", ex.getErrorCode());
            assertEquals(409, ex.getHttpStatus());
            assertTrue(ex.getMessage().contains("testuser"));
        }

        @Test
        @DisplayName("EMAIL_EXISTED với detail")
        void testEmailExisted() {
            DuplicateUserException ex = new DuplicateUserException(
                    DuplicateUserException.ErrorCode.EMAIL_EXISTED, "test@email.com");
            assertEquals("EMAIL_EXISTED", ex.getErrorCode());
            assertEquals(409, ex.getHttpStatus());
            assertTrue(ex.getMessage().contains("test@email.com"));
        }

        @Test
        @DisplayName("ErrorCode getMessage() không rỗng")
        void testErrorCodeMessages() {
            for (DuplicateUserException.ErrorCode code : DuplicateUserException.ErrorCode.values()) {
                assertNotNull(code.getMessage());
                assertFalse(code.getMessage().isBlank());
            }
        }

        @Test
        @DisplayName("DuplicateUserException kế thừa AppException")
        void testInheritance() {
            DuplicateUserException ex = new DuplicateUserException(
                    DuplicateUserException.ErrorCode.USERNAME_EXISTED, "user");
            assertInstanceOf(AppException.class, ex);
        }
    }

    // =========================================================================
    // InsufficientBalanceException
    // =========================================================================
    @Nested
    @DisplayName("InsufficientBalanceException")
    class InsufficientBalanceExceptionTests {

        @Test
        @DisplayName("Default constructor")
        void testDefaultConstructor() {
            InsufficientBalanceException ex = new InsufficientBalanceException();
            assertEquals("INSUFFICIENT_BALANCE", ex.getErrorCode());
            assertEquals(400, ex.getHttpStatus());
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("Constructor với currentBalance và requiredAmount")
        void testDetailConstructor() {
            BigDecimal current = new BigDecimal("100.00");
            BigDecimal required = new BigDecimal("500.00");
            InsufficientBalanceException ex = new InsufficientBalanceException(current, required);
            assertEquals("INSUFFICIENT_BALANCE", ex.getErrorCode());
            assertEquals(400, ex.getHttpStatus());
            assertTrue(ex.getMessage().contains("100.00"));
            assertTrue(ex.getMessage().contains("500.00"));
        }

        @Test
        @DisplayName("InsufficientBalanceException kế thừa AppException")
        void testInheritance() {
            InsufficientBalanceException ex = new InsufficientBalanceException();
            assertInstanceOf(AppException.class, ex);
        }
    }

    // =========================================================================
    // InvalidBidException
    // =========================================================================
    @Nested
    @DisplayName("InvalidBidException")
    class InvalidBidExceptionTests {

        @Test
        @DisplayName("Constructor với BidError")
        void testConstructorWithBidError() {
            InvalidBidException ex = new InvalidBidException(InvalidBidException.BidError.BID_TOO_LOW);
            assertEquals("BID_TOO_LOW", ex.getErrorCode());
            assertEquals(400, ex.getHttpStatus());
            assertEquals(InvalidBidException.BidError.BID_TOO_LOW.getMessage(), ex.getMessage());
        }

        @Test
        @DisplayName("Constructor với BidError và currentHighestBid")
        void testConstructorWithBidAmount() {
            BigDecimal highestBid = new BigDecimal("1000.00");
            InvalidBidException ex = new InvalidBidException(InvalidBidException.BidError.BID_TOO_LOW, highestBid);
            assertEquals("BID_TOO_LOW", ex.getErrorCode());
            assertTrue(ex.getMessage().contains("1000.00"));
        }

        @Test
        @DisplayName("Tất cả BidError có message không rỗng")
        void testAllBidErrors() {
            for (InvalidBidException.BidError error : InvalidBidException.BidError.values()) {
                InvalidBidException ex = new InvalidBidException(error);
                assertEquals(error.name(), ex.getErrorCode());
                assertEquals(400, ex.getHttpStatus());
                assertNotNull(error.getMessage());
                assertFalse(error.getMessage().isBlank());
            }
        }

        @Test
        @DisplayName("Kiểm tra các BidError cụ thể")
        void testSpecificBidErrors() {
            assertDoesNotThrow(() -> new InvalidBidException(InvalidBidException.BidError.AUCTION_ENDED));
            assertDoesNotThrow(() -> new InvalidBidException(InvalidBidException.BidError.INSUFFICIENT_FUNDS));
            assertDoesNotThrow(() -> new InvalidBidException(InvalidBidException.BidError.OWNER_CANNOT_BID));
            assertDoesNotThrow(() -> new InvalidBidException(InvalidBidException.BidError.INVALID_AMOUNT));
            assertDoesNotThrow(() -> new InvalidBidException(InvalidBidException.BidError.BID_LOWER_THAN_START_PRICE));
        }

        @Test
        @DisplayName("InvalidBidException kế thừa AppException")
        void testInheritance() {
            InvalidBidException ex = new InvalidBidException(InvalidBidException.BidError.BID_TOO_LOW);
            assertInstanceOf(AppException.class, ex);
        }
    }

    // =========================================================================
    // InvalidCredentialException
    // =========================================================================
    @Nested
    @DisplayName("InvalidCredentialException")
    class InvalidCredentialExceptionTests {

        @Test
        @DisplayName("Default constructor")
        void testDefaultConstructor() {
            InvalidCredentialException ex = new InvalidCredentialException();
            assertEquals("INVALID_CREDENTIALS", ex.getErrorCode());
            assertEquals(401, ex.getHttpStatus());
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("Constructor với username")
        void testUsernameConstructor() {
            InvalidCredentialException ex = new InvalidCredentialException("john_doe");
            assertEquals("INVALID_CREDENTIALS", ex.getErrorCode());
            assertEquals(401, ex.getHttpStatus());
            assertTrue(ex.getMessage().contains("john_doe"));
        }

        @Test
        @DisplayName("InvalidCredentialException kế thừa AppException")
        void testInheritance() {
            InvalidCredentialException ex = new InvalidCredentialException();
            assertInstanceOf(AppException.class, ex);
        }
    }

    // =========================================================================
    // ItemException
    // =========================================================================
    @Nested
    @DisplayName("ItemException")
    class ItemExceptionTests {

        @Test
        @DisplayName("Constructor với ErrorCode")
        void testConstructorWithErrorCode() {
            ItemException ex = new ItemException(ItemException.ErrorCode.ITEM_NOT_FOUND);
            assertEquals("ITEM_001", ex.getErrorCode());
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("Constructor với ErrorCode và detail")
        void testConstructorWithDetail() {
            ItemException ex = new ItemException(ItemException.ErrorCode.ITEM_NOT_FOUND, "id=99");
            assertTrue(ex.getMessage().contains("id=99"));
        }

        @Test
        @DisplayName("Tất cả ErrorCode có code và message")
        void testAllErrorCodes() {
            for (ItemException.ErrorCode code : ItemException.ErrorCode.values()) {
                assertNotNull(code.getCode());
                assertNotNull(code.getMessage());
                assertFalse(code.getCode().isBlank());
                assertFalse(code.getMessage().isBlank());
                // Tạo exception để đảm bảo constructor không lỗi
                ItemException ex = new ItemException(code);
                assertEquals(code.getCode(), ex.getErrorCode());
            }
        }

        @Test
        @DisplayName("Kiểm tra các ErrorCode cụ thể")
        void testSpecificCodes() {
            assertEquals("ITEM_002", new ItemException(ItemException.ErrorCode.ITEM_SAVE_FAILED).getErrorCode());
            assertEquals("ITEM_003", new ItemException(ItemException.ErrorCode.ITEM_FETCH_FAILED).getErrorCode());
            assertEquals("ITEM_004", new ItemException(ItemException.ErrorCode.INVALID_ITEM_DATA).getErrorCode());
            assertEquals("ITEM_005", new ItemException(ItemException.ErrorCode.FACTORY_CREATE_FAILED).getErrorCode());
        }

        @Test
        @DisplayName("ItemException kế thừa AppException")
        void testInheritance() {
            ItemException ex = new ItemException(ItemException.ErrorCode.ITEM_NOT_FOUND);
            assertInstanceOf(AppException.class, ex);
        }
    }

    // =========================================================================
    // OperationFailedException
    // =========================================================================
    @Nested
    @DisplayName("OperationFailedException")
    class OperationFailedExceptionTests {

        @Test
        @DisplayName("Constructor với message")
        void testConstructor() {
            OperationFailedException ex = new OperationFailedException("DB write failed");
            assertEquals("OPERATION_FAILED", ex.getErrorCode());
            assertEquals(500, ex.getHttpStatus());
            assertEquals("DB write failed", ex.getMessage());
        }

        @Test
        @DisplayName("OperationFailedException kế thừa AppException")
        void testInheritance() {
            OperationFailedException ex = new OperationFailedException("error");
            assertInstanceOf(AppException.class, ex);
        }
    }

    // =========================================================================
    // PermissionDeniedException
    // =========================================================================
    @Nested
    @DisplayName("PermissionDeniedException")
    class PermissionDeniedExceptionTests {

        @Test
        @DisplayName("Default constructor")
        void testDefaultConstructor() {
            PermissionDeniedException ex = new PermissionDeniedException();
            assertEquals("PERMISSION_DENIED", ex.getErrorCode());
            assertEquals(403, ex.getHttpStatus());
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("Constructor với custom message")
        void testCustomMessageConstructor() {
            PermissionDeniedException ex = new PermissionDeniedException("Admin only action");
            assertEquals("PERMISSION_DENIED", ex.getErrorCode());
            assertEquals(403, ex.getHttpStatus());
            assertEquals("Admin only action", ex.getMessage());
        }

        @Test
        @DisplayName("PermissionDeniedException kế thừa AppException")
        void testInheritance() {
            PermissionDeniedException ex = new PermissionDeniedException();
            assertInstanceOf(AppException.class, ex);
        }
    }

    // =========================================================================
    // UserNotFoundException
    // =========================================================================
    @Nested
    @DisplayName("UserNotFoundException")
    class UserNotFoundExceptionTests {

        @Test
        @DisplayName("Constructor với username")
        void testUsernameConstructor() {
            UserNotFoundException ex = new UserNotFoundException("alice");
            assertEquals("USER_NOT_FOUND", ex.getErrorCode());
            assertTrue(ex.getMessage().contains("alice"));
        }

        @Test
        @DisplayName("Constructor với ID (long)")
        void testIdConstructor() {
            UserNotFoundException ex = new UserNotFoundException(42L);
            assertEquals("USER_NOT_FOUND", ex.getErrorCode());
            assertTrue(ex.getMessage().contains("42"));
        }

        @Test
        @DisplayName("UserNotFoundException kế thừa AppException")
        void testInheritance() {
            UserNotFoundException ex = new UserNotFoundException("test");
            assertInstanceOf(AppException.class, ex);
        }

        @Test
        @DisplayName("Default HTTP status là 400")
        void testDefaultStatus() {
            UserNotFoundException ex = new UserNotFoundException("user");
            assertEquals(400, ex.getHttpStatus());
        }
    }
}