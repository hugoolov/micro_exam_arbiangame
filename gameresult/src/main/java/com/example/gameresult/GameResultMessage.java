package com.example.gameresult;

import java.io.Serializable;

public class GameResultMessage implements Serializable {
    private String playerName;
    private int playerScore;
    private int computerScore;
    private int rounds;

    public GameResultMessage() {}

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