package com.example.gameresult;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SaveResultRequest {
    private String playerName;
    private int playerScore;
    private int computerScore;
    private int rounds;
}