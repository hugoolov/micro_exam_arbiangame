package com.example.game_logic.savegame;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedGameRepository extends JpaRepository<SavedGame, Long> {

    List<SavedGame> findByPlayerNameOrderBySavedAtDesc(String playerName);

    List<SavedGame> findAllByOrderBySavedAtDesc();
}