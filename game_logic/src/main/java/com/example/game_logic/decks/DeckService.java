package com.example.game_logic.decks;

import com.example.game_logic.card.Card;
import com.example.game_logic.card.CardRepo;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DeckService {

    private final DeckRepo deckRepo;
    private final CardRepo cardRepo;

    public DeckService(DeckRepo deckRepo, CardRepo cardRepo) {
        this.deckRepo = deckRepo;
        this.cardRepo = cardRepo;
    }

    public Deck createDeck(String name, List<Long> cardIds) {
        Deck deck = new Deck();
        deck.setDeckName(name);
        deck.setCardIds(cardIds);
        deckRepo.save(deck);
        return populateDeckCards(deck);
    }

    public Deck getDeck(Long id) {
        return deckRepo.findById(id).map(this::populateDeckCards).orElse(null);
    }

    public Deck saveDeck(Deck deck) {
        deckRepo.save(deck);
        return populateDeckCards(deck);
    }

    /** Draw a card from the top of the deck */
    public List<Long> drawCards(Long deckId, int count) {
        Deck deck = deckRepo.findById(deckId).orElse(null);
        if (deck == null || deck.getCardIds().isEmpty()) return Collections.emptyList();

        List<Long> cardIds = deck.getCardIds();
        // Ensure we don't draw more cards than are available
        int actualCount = Math.min(count, cardIds.size());

        // Extract the first 'actualCount' card IDs
        List<Long> drawnCardIds = new ArrayList<>(cardIds.subList(0, actualCount));

        // Remove the drawn cards from the deck
        cardIds.subList(0, actualCount).clear();
        deckRepo.save(deck);

        // Return the drawn card IDs
        return drawnCardIds;
    }


    /** Shuffle a deck */
    public Deck shuffleDeck(Long deckId) {
        Deck deck = deckRepo.findById(deckId).orElse(null);
        if (deck == null) return null;

        Collections.shuffle(deck.getCardIds());
        return deckRepo.save(deck);
    }
    public Deck addCardToDeck(Long deckId, Card card) {
        Deck deck = deckRepo.findById(deckId)
                .orElseThrow(() -> new RuntimeException("Deck not found with id: " + deckId));

        deck.getCardIds().add(card.getId());
        return deckRepo.save(deck);
    }
    public Card getCardFromDeck(Long deckId, int index) {
        Deck deck = deckRepo.findById(deckId)
                .orElseThrow(() -> new RuntimeException("Deck not found with id: " + deckId));

        if (index < 0 || index >= deck.getCardIds().size()) {
            throw new IndexOutOfBoundsException("Invalid card index: " + index);
        }

        Long cardId = deck.getCardIds().get(index);
        return cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + cardId));
    }

    /** Populate deck with full Card objects */
    public Deck populateDeckCards(Deck deck) {
        if (deck.getCardIds() != null && !deck.getCardIds().isEmpty()) {
            List<Card> cards = cardRepo.findAllById(deck.getCardIds());

            // Sort cards to match the order of cardIds
            Map<Long, Card> cardMap = new HashMap<>();
            for (Card card : cards) {
                cardMap.put(card.getId(), card);
            }

            List<Card> orderedCards = new ArrayList<>();
            for (Long cardId : deck.getCardIds()) {
                orderedCards.add(cardMap.get(cardId));
            }

            deck.setCards(orderedCards);
        } else {
            deck.setCards(new ArrayList<>());
        }
        return deck;
    }


    public Deck removeDeckCards(Long deckId, Long cardId) {
        Deck deck = deckRepo.findById(deckId)
                .orElseThrow(() -> new RuntimeException("Deck not found with id: " + deckId));

        deck.getCardIds().removeIf(id -> id.equals(cardId));
        return deckRepo.save(deck);
    }
}
