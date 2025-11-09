package com.example.gameresult;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/results")
public class GameResultController {

    private final GameResultService gameResultService;

    public GameResultController(GameResultService gameResultService) {
        this.gameResultService = gameResultService;
    }

    @PostMapping
    public ResponseEntity<GameResult> saveResult(@RequestBody SaveResultRequest request) {
        GameResult result = gameResultService.saveGameResult(
                request.getPlayerName(),
                request.getPlayerScore(),
                request.getComputerScore(),
                request.getRounds()
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<GameResult>> getAllResults() {
        return ResponseEntity.ok(gameResultService.getAllResults());
    }
}