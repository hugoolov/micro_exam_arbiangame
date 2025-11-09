package com.example.game_logic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GameLogicApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameLogicApplication.class, args);
    }
}

