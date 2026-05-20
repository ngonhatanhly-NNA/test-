package com.server.exception;

import java.math.BigDecimal;

/**
 * Exception được ném ra khi người dùng không có đủ số dư trong ví để thực hiện một hành động,
 * ví dụ như đặt giá hoặc thực hiện một giao dịch mua bán.
 * Thường dẫn đến mã lỗi HTTP 400 Bad Request.
 */
public class InsufficientBalanceException extends AppException {

    /**
     * Constructor mặc định với thông báo chung.
     */
    public InsufficientBalanceException() {
        super("INSUFFICIENT_BALANCE", "Số dư trong ví không đủ để thực hiện giao dịch này.", 400);
    }

    /**
     * Constructor chi tiết, cung cấp thông tin về số dư hiện tại và số tiền yêu cầu.
     * Điều này rất hữu ích cho việc gỡ lỗi và cung cấp thông báo rõ ràng cho người dùng.
     *
     * @param currentBalance Số dư hiện tại của người dùng.
     * @param requiredAmount Số tiền cần thiết để thực hiện giao dịch.
     */
    public InsufficientBalanceException(BigDecimal currentBalance, BigDecimal requiredAmount) {
        super(
            "INSUFFICIENT_BALANCE",
            String.format(
                "Giao dịch thất bại. Số dư không đủ. Hiện có: %s, Yêu cầu: %s",
                currentBalance.toPlainString(),
                requiredAmount.toPlainString()
            ),
            400
        );
    }
}
