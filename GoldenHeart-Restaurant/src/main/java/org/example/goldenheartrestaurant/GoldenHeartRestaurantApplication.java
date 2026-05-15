package org.example.goldenheartrestaurant;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Điểm khởi động của backend GoldenHeart Restaurant.
 *
 * Lớp này có vai trò nối toàn bộ nền tảng Spring Boot lại với nhau:
 * - quét {@code @ConfigurationProperties} để bind cấu hình typed-safe
 * - quét entity và repository từ các module nghiệp vụ
 * - trao quyền bootstrap cho Spring Boot để dựng toàn bộ application context
 *
 * Việc scan theo package {@code modules} giúp codebase giữ được cấu trúc
 * domain-first: mỗi mảng nghiệp vụ tự gom entity, repository, service, controller
 * của chính nó thay vì dồn hết theo technical layer.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EntityScan(basePackages = "org.example.goldenheartrestaurant.modules")
@EnableJpaRepositories(basePackages = "org.example.goldenheartrestaurant.modules")
public class GoldenHeartRestaurantApplication {

    public static void main(String[] args) {
        // Từ đây Spring Boot bắt đầu quét bean, dựng cấu hình và mở web server.
        SpringApplication.run(GoldenHeartRestaurantApplication.class, args);
    }

}
