package org.example.goldenheartrestaurant.modules.customerportal.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Dữ liệu cập nhật hồ sơ do khách hàng tự chỉnh sửa.
 * Chỉ cho phép sửa thông tin cá nhân — email và điểm tích luỹ không thể tự sửa.
 */
public record UpdateCustomerProfileRequest(

        @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
        String name,

        @Pattern(regexp = "^(\\+84|0)[0-9]{8,9}$", message = "Số điện thoại không hợp lệ")
        String phone,

        @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
        String address,

        LocalDate dateOfBirth,

        @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Giới tính phải là MALE, FEMALE hoặc OTHER")
        String gender
) {
}
