package com.example.game_logic.decks;

import com.example.game_logic.card.Card;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Deck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deckId;

    private String deckName;

    // Database stores only IDs
    @ElementCollection
    @CollectionTable(name = "deck_card_ids", joinColumns = @JoinColumn(name = "deck_id"))
    @Column(name = "card_id")
    private List<Long> cardIds = new ArrayList<>();

    // Runtime only: populated when sending to frontend
    @Transient
    private List<Card> cards = new ArrayList<>();
}
