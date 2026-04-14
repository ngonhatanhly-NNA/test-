package com.server.exception;

public class AuctionAppException extends AppException {
    public AuctionAppException(String errorCode, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
    }

    public static AuctionAppException from(AuctionException e) {
        AuctionException.ErrorCode code = e.getErrorCode();
        int status = switch (code) {
            case AUCTION_NOT_FOUND, AUCTION_NOT_ACTIVE -> 404;
            case INVALID_BID_AMOUNT, BID_AMOUNT_TOO_LOW, INVALID_AUTO_BID_CONFIG -> 400;
            case AUCTION_ALREADY_FINISHED -> 410;
            default -> 500;
        };
        return new AuctionAppException(code.name(), e.getMessage(), status);
    }
}

