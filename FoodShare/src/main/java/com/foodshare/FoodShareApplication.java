package com.foodshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan("com.foodshare.entity")
@EnableJpaRepositories("com.foodshare.repository")
public class FoodShareApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodShareApplication.class, args);
    }
}
