package org.example.goldenheartrestaurant;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ConfigurationPropertiesScan
@EntityScan(basePackages = "org.example.goldenheartrestaurant.modules")
@EnableJpaRepositories(basePackages = "org.example.goldenheartrestaurant.modules")
public class GoldenHeartRestaurantApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoldenHeartRestaurantApplication.class, args);
    }

}
