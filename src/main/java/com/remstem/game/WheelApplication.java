package com.remstem.game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WheelApplication {
    public static void main(String[] args) {
        SpringApplication.run(WheelApplication.class, args);
    }
}
