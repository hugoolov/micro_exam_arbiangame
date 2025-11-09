package com.example.game_logic.gamestate;

import com.example.game_logic.card.Card;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CompleteTurnRequest {
    private Card drawnCard;
    private boolean swap;
    private Integer cardIndexToSwap;
    private String drawFrom;
}