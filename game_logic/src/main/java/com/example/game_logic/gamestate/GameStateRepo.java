package com.example.game_logic.gamestate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameStateRepo extends JpaRepository<GameState, Long> {
}
