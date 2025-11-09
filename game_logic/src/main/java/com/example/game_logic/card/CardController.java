package com.example.game_logic.card;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    CardService cardService;
    public CardController(CardService cardService) {
        this.cardService = cardService;
    }
    @PostMapping("/init")
    public ResponseEntity<String> initCards() {
        cardService.initCards();
        return ResponseEntity.ok("52 cards initialized successfully.");
    }

}
