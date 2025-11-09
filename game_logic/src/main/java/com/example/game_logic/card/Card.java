package com.example.game_logic.card;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "card_value")
    private int value;

    @Enumerated(EnumType.STRING)  // ensures enum stored as text, not ordinal
    private Suite suite;

    private String filename;
}
