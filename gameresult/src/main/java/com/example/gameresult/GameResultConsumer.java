package com.example.gameresult;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class GameResultConsumer {

    private final GameResultService gameResultService;

    public GameResultConsumer(GameResultService gameResultService) {
        this.gameResultService = gameResultService;
    }

    @RabbitListener(queues = "game-result-queue")
    public void receiveGameResult(GameResultMessage message) {
        System.out.println("Received game result: " + message.getPlayerName());
        System.out.println("Player score: " + message.getPlayerScore());
        System.out.println("Computer score: " + message.getComputerScore());
        System.out.println("Rounds: " + message.getRounds());

        GameResult saved = gameResultService.saveGameResult(
                message.getPlayerName(),
                message.getPlayerScore(),
                message.getComputerScore(),
                message.getRounds()
        );

        System.out.println("Saved result with ID: " + saved.getId());
    }
}