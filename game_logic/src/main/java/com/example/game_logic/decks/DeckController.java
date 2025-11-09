package com.example.game_logic.decks;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @PostMapping
    public ResponseEntity<Deck> createDeck(@RequestBody DeckDTO request) {
        Deck deck = deckService.createDeck(request.deckName(), request.cardIds());
        return ResponseEntity.ok(deck);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Deck> getDeck(@PathVariable Long id) {
        Deck deck = deckService.getDeck(id);
        return deck != null ? ResponseEntity.ok(deck) : ResponseEntity.notFound().build();
    }

    @PostMapping("/save")
    public ResponseEntity<Deck> saveDeck(@RequestBody Deck deck) {
        return ResponseEntity.ok(deckService.saveDeck(deck));
    }


    @PostMapping("/{deckId}/remove/{cardId}")
    public ResponseEntity<Deck> removeDeckCards(@PathVariable Long deckId, @PathVariable Long cardId) {
        Deck updatedDeck = deckService.removeDeckCards(deckId, cardId);
        return ResponseEntity.ok(updatedDeck);
    }

    @PostMapping("/{id}/draw")
    public ResponseEntity<List<Long>> drawCards(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int count) {

        List<Long> drawnCardIds = deckService.drawCards(id, count);

        if (drawnCardIds.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(drawnCardIds);
    }


    @PostMapping("/{id}/shuffle")
    public ResponseEntity<Deck> shuffleDeck(@PathVariable Long id) {
        Deck deck = deckService.shuffleDeck(id);
        return deck != null ? ResponseEntity.ok(deck) : ResponseEntity.notFound().build();
    }
}
