package com.flashlearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlashLearnApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashLearnApplication.class, args);
    }
}
