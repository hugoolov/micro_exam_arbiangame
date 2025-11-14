package com.example.game_logic.gamestate;

import com.example.game_logic.card.Card;
import com.example.game_logic.savegame.SavedGame;
import com.example.game_logic.savegame.SavedGameResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Save current game state
     * POST /api/game/{gameId}/save
     * Body: { "playerName": "...", "saveName": "..." }
     */
    @PostMapping("/{gameId}/save")
    public ResponseEntity<SavedGameResponse> saveGame(
            @PathVariable Long gameId,
            @RequestBody Map<String, String> request) {

        String playerName = request.get("playerName");
        String saveName = request.get("saveName");

        SavedGame savedGame = gameStateService.saveGame(gameId, playerName, saveName);
        SavedGameResponse response = convertToResponse(savedGame);

        return ResponseEntity.ok(response);
    }

    /**
     * Load a saved game
     * POST /api/game/load/{savedGameId}
     */
    @PostMapping("/load/{savedGameId}")
    public ResponseEntity<GameStateResponse> loadSavedGame(@PathVariable Long savedGameId) {
        GameState gameState = gameStateService.loadSavedGame(savedGameId);
        GameStateResponse response = gameStateService.getGameStateResponse(gameState.getGameId());
        return ResponseEntity.ok(response);
    }

    /**
     * Get all saved games for a player
     * GET /api/game/saved?playerName=...
     */
    @GetMapping("/saved")
    public ResponseEntity<List<SavedGameResponse>> getSavedGames(
            @RequestParam(required = false) String playerName) {

        List<SavedGame> savedGames = gameStateService.getSavedGames(playerName);
        List<SavedGameResponse> responses = savedGames.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Delete a saved game
     * DELETE /api/game/saved/{savedGameId}
     */
    @DeleteMapping("/saved/{savedGameId}")
    public ResponseEntity<Void> deleteSavedGame(@PathVariable Long savedGameId) {
        gameStateService.deleteSavedGame(savedGameId);
        return ResponseEntity.ok().build();
    }

    /**
     * Convert SavedGame entity to SavedGameResponse DTO
     */
    private SavedGameResponse convertToResponse(SavedGame savedGame) {
        SavedGameResponse response = new SavedGameResponse();
        response.setId(savedGame.getId());
        response.setPlayerName(savedGame.getPlayerName());
        response.setSaveName(savedGame.getSaveName());
        response.setSavedAt(savedGame.getSavedAt());

        GameState gameState = savedGame.getGameState();
        if (gameState != null) {
            response.setRoundNumber(gameState.getRoundNumber());
            response.setPlayerScore(gameState.getPlayerScore());
            response.setComputerScore(gameState.getComputerScore());
            response.setGameStateId(gameState.getGameId());
        }

        return response;
    }
}