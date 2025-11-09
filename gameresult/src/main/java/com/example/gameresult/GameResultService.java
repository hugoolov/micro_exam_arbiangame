package com.example.gameresult;


import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GameResultService {

    private final GameResultRepo gameResultRepo;

    public GameResultService(GameResultRepo gameResultRepo) {
        this.gameResultRepo = gameResultRepo;
    }

    public GameResult saveGameResult(String playerName, int playerScore, int computerScore, int rounds) {
        GameResult result = new GameResult();
        result.setPlayerName(playerName);
        result.setPlayerScore(playerScore);
        result.setComputerScore(computerScore);
        result.setRounds(rounds);
        result.setGameDate(LocalDateTime.now());

        // Determine winner
        if (playerScore < computerScore) {
            result.setWinner("PLAYER");
        } else if (computerScore < playerScore) {
            result.setWinner("COMPUTER");
        } else {
            result.setWinner("TIE");
        }

        return gameResultRepo.save(result);
    }

    public List<GameResult> getAllResults() {
        return gameResultRepo.findAllByOrderByGameDateDesc();
    }
}
