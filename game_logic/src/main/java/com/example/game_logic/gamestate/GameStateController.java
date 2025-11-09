// GameStateController.java
package com.example.game_logic.gamestate;

import com.example.game_logic.card.Card;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
public class GameStateController {

    private final GameStateService gameStateService;

    public GameStateController(GameStateService gameStateService) {
        this.gameStateService = gameStateService;
    }

    /**
     * Start a new game
     * POST /api/game/start
     */
    @PostMapping("/start")
    public ResponseEntity<GameStateResponse> startGame() {
        GameState gameState = gameStateService.initializeGame();
        GameStateResponse response = gameStateService.getGameStateResponse(gameState.getGameId());
        return ResponseEntity.ok(response);
    }

    /**
     * Draw a card (Step 1 of turn)
     * POST /api/game/{gameId}/draw?from=mainDeck
     */
    @PostMapping("/{gameId}/draw")
    public ResponseEntity<Card> drawCard(
            @PathVariable Long gameId,
            @RequestParam String from) {

        Card drawnCard = gameStateService.drawCard(gameId, from);
        return ResponseEntity.ok(drawnCard);
    }

    /**
     * Complete the turn with player's decision (Step 2 of turn)
     * POST /api/game/{gameId}/complete-turn
     * Body: { "drawnCard": {...}, "swap": true, "cardIndexToSwap": 2 }
     */
    @PostMapping("/{gameId}/complete-turn")
    public ResponseEntity<GameStateResponse> completeTurn(
            @PathVariable Long gameId,
            @RequestBody CompleteTurnRequest request) {

        System.out.println("Controller received - swap: " + request.isSwap() + ", cardIndexToSwap: " + request.getCardIndexToSwap());
        System.out.println("DrawnCard id: " + (request.getDrawnCard() != null ? request.getDrawnCard().getId() : "null"));

        GameStateResponse response = gameStateService.completeTurn(
                gameId,
                request.getDrawnCard(),
                request.isSwap(),
                request.getCardIndexToSwap(),
                request.getDrawFrom()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get current game state without making a move
     * GET /api/game/{gameId}
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameStateResponse> getGameState(@PathVariable Long gameId) {
        GameStateResponse response = gameStateService.getGameStateResponse(gameId);
        return ResponseEntity.ok(response);
    }

    /**
     * End  a game
     * DELETE /api/game/{gameId}
     */
    @PostMapping("/{gameId}/end")
    public ResponseEntity<GameStateResponse> endGame(@PathVariable Long gameId) {
        GameStateResponse response = gameStateService.endGameManually(gameId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gameId}/save-result")
    public ResponseEntity<Void> saveGameResult(
            @PathVariable Long gameId,
            @RequestBody Map<String, String> request) {

        String playerName = request.get("playerName");
        gameStateService.saveGameResultViaQueue(gameId, playerName);

        return ResponseEntity.ok().build();
    }
}
