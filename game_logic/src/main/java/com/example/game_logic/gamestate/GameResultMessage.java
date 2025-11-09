package com.example.game_logic.gamestate;

import java.io.Serializable;

public class GameResultMessage implements Serializable {
    private String playerName;
    private int playerScore;
    private int computerScore;
    private int rounds;

    public GameResultMessage() {}

    public GameResultMessage(String playerName, int playerScore, int computerScore, int rounds) {
        this.playerName = playerName;
        this.playerScore = playerScore;
        this.computerScore = computerScore;
        this.rounds = rounds;
    }

    // Getters and setters
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getPlayerScore() { return playerScore; }
    public void setPlayerScore(int playerScore) { this.playerScore = playerScore; }
    public int getComputerScore() { return computerScore; }
    public void setComputerScore(int computerScore) { this.computerScore = computerScore; }
    public int getRounds() { return rounds; }
    public void setRounds(int rounds) { this.rounds = rounds; }
}