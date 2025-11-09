package com.example.game_logic.card;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CardService {
    private final CardRepo cardRepo;
    public CardService(CardRepo cardRepo) {
        this.cardRepo = cardRepo;
    }

    private static final Map<Integer, String> RANKS = Map.ofEntries(
            Map.entry(1, "ace"),
            Map.entry(11, "jack"),
            Map.entry(12, "queen"),
            Map.entry(13, "king")
    );

    @PostConstruct
    public void init() {
        initCards();
    }

    public void initCards() {
        if (cardRepo.count() == 52) return; // Already initialized

        List<Card> cards = new ArrayList<>();
        for (Suite suite : Suite.values()) {
            for (int value = 1; value <= 13; value++) {
                Card card = new Card();
                card.setValue(value);
                card.setSuite(suite);
                String rank = RANKS.getOrDefault(value, String.valueOf(value));
                card.setFilename(rank + "_of_" + suite.name().toLowerCase() + ".svg");

                cards.add(card);
            }
        }
        cardRepo.saveAll(cards);
    }

    public List<Card> getAllCards(){
        return cardRepo.findAll();
    }

    public Card getCardById(Long cardId){
        return cardRepo.findById(cardId).orElse(null);
    }

    /**
     * Calculate the score for a card based on game rules:
     * - 10 of diamonds: -10
     * - 10 of hearts: -10
     * - All aces: -5
     * - All kings: 0
     * - 2-9: face value
     * - 10s (except diamonds/hearts), Jacks, Queens: 10
     */
    public int calculateCardScore(Card card) {
        int value = card.getValue();
        Suite suite = card.getSuite();

        // Aces are -5
        if (value == 1) {
            return -5;
        }

        // Kings are 0
        if (value == 13) {
            return 0;
        }

        // 10 of diamonds and 10 of hearts are -10
        if (value == 10 && (suite == Suite.DIAMONDS || suite == Suite.HEARTS)) {
            return -10;
        }

        // 2-9 are face value
        if (value >= 2 && value <= 9) {
            return value;
        }

        // 10s (clubs/spades), Jacks (11), Queens (12) are 10
        return 10;
    }

    /**
     * Calculate total score for a list of cards
     */
    public int calculateHandScore(List<Card> cards) {
        return cards.stream()
                .mapToInt(this::calculateCardScore)
                .sum();
    }
}
