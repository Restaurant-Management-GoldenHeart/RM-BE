package org.example.goldenheartrestaurant.common.exception;

/**
 * Lỗi đầu vào / trạng thái request không hợp lệ nhưng không thuộc nhóm validation bean.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
