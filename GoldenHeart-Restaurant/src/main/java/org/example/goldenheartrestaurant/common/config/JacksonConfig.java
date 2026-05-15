package org.example.goldenheartrestaurant.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình {@link ObjectMapper} dùng chung cho toàn bộ ứng dụng.
 *
 * Bean này được tiêm vào các service cần:
 * - serialize payload webhook để lưu audit/debug
 * - xử lý {@code LocalDateTime}, {@code Optional} và các kiểu Java hiện đại
 * - tránh việc mỗi nơi tự tạo một {@code ObjectMapper} khác nhau rồi sinh lệch hành vi
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Tự động đăng ký các module Jackson phổ biến như Java Time,
        // giúp serialize/deserialize ổn định hơn mà không cần cấu hình rải rác.
        return new ObjectMapper().findAndRegisterModules();
    }
}
