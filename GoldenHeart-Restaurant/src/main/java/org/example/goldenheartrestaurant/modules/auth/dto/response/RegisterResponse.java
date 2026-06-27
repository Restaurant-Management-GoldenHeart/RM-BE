package org.example.goldenheartrestaurant.modules.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RegisterResponse {

    private Integer userId;

    private String username;

    private String email;

    private String fullName;

    private String role;

    private LocalDateTime createdAt;

    /**
     * true nếu tài khoản vừa đăng ký được tự động liên kết với hồ sơ khách hàng CRM đã tồn tại
     * (ví dụ: khách đã tích điểm qua POS trước đó).
     * Frontend dùng để hiển thị thông báo "Đã khôi phục X điểm từ hồ sơ thành viên cũ".
     */
    private boolean existingCrmLinked;

    /** Số điểm tích lũy được kế thừa từ CRM cũ (chỉ có giá trị khi existingCrmLinked = true). */
    private Integer inheritedLoyaltyPoints;
}
