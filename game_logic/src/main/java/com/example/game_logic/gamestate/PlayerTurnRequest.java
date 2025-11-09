// PlayerTurnRequest.java
package com.example.game_logic.gamestate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PlayerTurnRequest {
    private String drawFrom; // "mainDeck" or "openTable"
    private boolean swap;
    private Integer cardIndexToSwap; // Can be null if swap is false
}