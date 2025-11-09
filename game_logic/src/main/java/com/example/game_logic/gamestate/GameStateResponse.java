package com.example.game_logic.gamestate;

import com.example.game_logic.card.Card;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GameStateResponse {
    private Long gameId;
    private List<Card> playerHand;
    private int computerHandSize; // Don't reveal computer's cards
    private Card drawnCard; // The card that was just drawn (null if just getting state)
    private Card topOpenTableCard; // The top card on the open table deck
    private int mainDeckSize;
    private int openTableSize;
    private int roundNumber;
    private boolean gameOver;
    private String message;
    private int playerScore;
    private int computerScore;
}
