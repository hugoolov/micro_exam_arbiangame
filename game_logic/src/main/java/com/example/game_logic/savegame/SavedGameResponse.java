package com.example.game_logic.savegame;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SavedGameResponse {
    private Long id;
    private String playerName;
    private String saveName;
    private LocalDateTime savedAt;
    private int roundNumber;
    private int playerScore;
    private int computerScore;
    private Long gameStateId;
}