package com.example.gameresult;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameResultRepo extends JpaRepository<GameResult, Long> {
    List<GameResult> findAllByOrderByGameDateDesc();
}