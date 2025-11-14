package com.example.game_logic.gamestate;

import com.example.game_logic.card.Card;
import com.example.game_logic.card.CardService;
import com.example.game_logic.config.RabbitMQConfig;
import com.example.game_logic.decks.Deck;
import com.example.game_logic.decks.DeckService;
import com.example.game_logic.savegame.SavedGame;
import com.example.game_logic.savegame.SavedGameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class GameStateService {

    private final CardService cardService;
    private final DeckService deckService;
    private final GameStateRepo gameStateRepo;
    private final RabbitTemplate rabbitTemplate;
    private final SavedGameRepository savedGameRepository;

    public GameStateService(CardService cardService, DeckService deckService, GameStateRepo gameStateRepo, RabbitTemplate rabbitTemplate, SavedGameRepository savedGameRepository) {
        this.cardService = cardService;
        this.deckService = deckService;
        this.gameStateRepo = gameStateRepo;
        this.rabbitTemplate = rabbitTemplate;
        this.savedGameRepository = savedGameRepository;
    }

    /**
     * Initialize a new game with shuffled deck and dealt hands
     */
    @Transactional
    public GameState initializeGame() {
        // Create card IDs for a full deck
        List<Long> cardIds = new ArrayList<>();
        for (long i = 1; i <= 52; i++) {
            cardIds.add(i);
        }

        // Create and shuffle the main deck
        Deck mainDeck = deckService.createDeck("mainDeck", cardIds);
        deckService.shuffleDeck(mainDeck.getDeckId());

        // Create hands - drawCards already removes from deck
        List<Long> playerCardIds = deckService.drawCards(mainDeck.getDeckId(), 4);
        Deck playerHand = deckService.createDeck("playerHand", playerCardIds);

        List<Long> computerCardIds = deckService.drawCards(mainDeck.getDeckId(), 4);
        Deck computerHand = deckService.createDeck("computerHand", computerCardIds);

        // Create empty open table deck
        Deck openTableDeck = deckService.createDeck("openTableDeck", new ArrayList<>());

        // Create and save game state
        GameState gameState = new GameState();
        gameState.setMainDeck(mainDeck);
        gameState.setPlayerHand(playerHand);
        gameState.setComputerHand(computerHand);
        gameState.setOpenTableDeck(openTableDeck);
        gameState.setPlayerScore(0);
        gameState.setComputerScore(0);
        gameState.setRoundNumber(1);
        gameState.setGameOver(false);

        return gameStateRepo.save(gameState);
    }

    /**
     * Computer AI logic with improved strategy:
     * 1. Check if open table card is better than worst card in hand - if so, take it
     * 2. Otherwise draw from main deck
     * 3. Swap if drawn card has better score than worst card in hand
     */
    private String executeComputerTurn(GameState gameState) {
        Deck mainDeck = deckService.getDeck(gameState.getMainDeck().getDeckId());
        Deck openTableDeck = deckService.getDeck(gameState.getOpenTableDeck().getDeckId());
        Deck computerHand = deckService.getDeck(gameState.getComputerHand().getDeckId());

        Card drawnCard = null;
        String drawSource = "";

        // Strategy 1: Check if open table has a beneficial card
        if (!openTableDeck.getCardIds().isEmpty()) {
            Long topCardId = openTableDeck.getCardIds().get(openTableDeck.getCardIds().size() - 1);
            Card topOpenCard = cardService.getCardById(topCardId);
            int topCardScore = cardService.calculateCardScore(topOpenCard);

            // Find worst (highest score) card in computer's hand
            int worstScore = Integer.MIN_VALUE;
            for (Card card : computerHand.getCards()) {
                int score = cardService.calculateCardScore(card);
                if (score > worstScore) {
                    worstScore = score;
                }
            }

            // Take from open table if it improves hand
            if (topCardScore < worstScore) {
                deckService.removeDeckCards(openTableDeck.getDeckId(), topCardId);
                drawnCard = topOpenCard;
                drawSource = "open table";
            }
        }

        // Strategy 2: If didn't take from open table, draw from main deck
        if (drawnCard == null) {
            if (mainDeck.getCardIds().isEmpty()) {
                return "Computer cannot draw - deck empty. ";
            }

            List<Long> computerDrawnIds = deckService.drawCards(mainDeck.getDeckId(), 1);
            if (computerDrawnIds.isEmpty()) {
                return "Computer cannot draw - deck empty. ";
            }

            drawnCard = cardService.getCardById(computerDrawnIds.get(0));
            drawSource = "main deck";
        }

        String computerMessage = "Computer drew " + drawnCard.getValue() +
                " of " + drawnCard.getSuite() + " from " + drawSource + ". ";

        // Strategy 3: Decide whether to swap based on scores
        int drawnCardScore = cardService.calculateCardScore(drawnCard);

        Card worstCard = null;
        int worstScore = Integer.MIN_VALUE;

        // Find the worst (highest score) card in hand
        for (Card card : computerHand.getCards()) {
            int cardScore = cardService.calculateCardScore(card);
            if (cardScore > worstScore) {
                worstScore = cardScore;
                worstCard = card;
            }
        }

        // Swap if drawn card is better (lower score) than worst card in hand
        if (worstCard != null && drawnCardScore < worstScore) {
            deckService.removeDeckCards(computerHand.getDeckId(), worstCard.getId());
            deckService.addCardToDeck(openTableDeck.getDeckId(), worstCard);
            deckService.addCardToDeck(computerHand.getDeckId(), drawnCard);
            computerMessage += "Computer swapped out " + worstCard.getValue() +
                    " of " + worstCard.getSuite() + ".";
        } else {
            deckService.addCardToDeck(openTableDeck.getDeckId(), drawnCard);
            computerMessage += "Computer discarded the card.";
        }

        return computerMessage;
    }

    /**
     * Check if game should end and update game state accordingly
     */
    /**
     * Calculate and save final scores when game ends
     */
    private void calculateFinalScores(GameState gameState) {
        Deck playerHand = deckService.getDeck(gameState.getPlayerHand().getDeckId());
        Deck computerHand = deckService.getDeck(gameState.getComputerHand().getDeckId());

        int playerFinalScore = cardService.calculateHandScore(playerHand.getCards());
        int computerFinalScore = cardService.calculateHandScore(computerHand.getCards());

        gameState.setPlayerScore(playerFinalScore);
        gameState.setComputerScore(computerFinalScore);
    }
    private void checkGameEnd(GameState gameState) {
        Deck mainDeck = deckService.getDeck(gameState.getMainDeck().getDeckId());

        // Game ends when no cards left to draw
        if (mainDeck.getCardIds().isEmpty()) {
            gameState.setGameOver(true);
            calculateFinalScores(gameState);
        }
    }
    /**
     * Draw a card from main deck or open table (Step 1 of player turn)
     */
    @Transactional
    public Card drawCard(Long gameId, String drawFrom) {
        GameState gameState = gameStateRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found with id: " + gameId));

        if (gameState.isGameOver()) {
            throw new RuntimeException("Game is already over!");
        }

        // Validate drawFrom parameter
        if (drawFrom == null || (!drawFrom.equals("mainDeck") && !drawFrom.equals("openTable"))) {
            throw new RuntimeException("Invalid draw source! Must be 'mainDeck' or 'openTable'.");
        }

        Deck mainDeck = deckService.getDeck(gameState.getMainDeck().getDeckId());
        Deck openTableDeck = deckService.getDeck(gameState.getOpenTableDeck().getDeckId());

        Card drawnCard;

        if ("openTable".equals(drawFrom)) {
            // Just LOOK at the card, don't remove it yet
            List<Long> openTableCardIds = openTableDeck.getCardIds();
            Long drawnCardId = openTableCardIds.get(openTableCardIds.size() - 1);
            drawnCard = cardService.getCardById(drawnCardId);
            // DON'T remove it here - do it in completeTurn
        } else {
            // Just LOOK at the card, don't remove it yet
            List<Long> cardIds = mainDeck.getCardIds();
            if (cardIds.isEmpty()) {
                throw new RuntimeException("Game over - no more cards in deck!");
            }
            Long drawnCardId = cardIds.get(0);
            drawnCard = cardService.getCardById(drawnCardId);
            // DON'T remove it here - do it in completeTurn
        }

        return drawnCard;
    }

    /**
     * Complete the turn with player's decision and computer's turn (Step 2 of player turn)
     */
    @Transactional
    public GameStateResponse completeTurn(Long gameId, Card drawnCard, boolean playerSwaps, Integer cardIndexToSwap, String drawFrom) {

        System.out.println("Service received - playerSwaps: " + playerSwaps + ", cardIndexToSwap: " + cardIndexToSwap);
        System.out.println("DrawnCard id: " + (drawnCard != null ? drawnCard.getId() : "null"));

        GameState gameState = gameStateRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found with id: " + gameId));

        if (gameState.isGameOver()) {
            return buildResponse(gameState, null, "Game is already over!");
        }

        Deck playerHand = deckService.getDeck(gameState.getPlayerHand().getDeckId());
        Deck openTableDeck = deckService.getDeck(gameState.getOpenTableDeck().getDeckId());
        Deck mainDeck = deckService.getDeck(gameState.getMainDeck().getDeckId());

        // NOW remove the card from its source
        if ("openTable".equals(drawFrom)) {
            deckService.removeDeckCards(openTableDeck.getDeckId(), drawnCard.getId());
        } else {
            deckService.drawCards(mainDeck.getDeckId(), 1); // removes from top
        }

        String message = "Player drew " + drawnCard.getValue() + " of " + drawnCard.getSuite() + ". ";

        // Handle player's swap decision
        // Handle player's swap decision
        if (playerSwaps && cardIndexToSwap != null) {
            if (cardIndexToSwap >= 0 && cardIndexToSwap < playerHand.getCards().size()) {
                System.out.println("Player hand before swap: " + playerHand.getCards());
                System.out.println("Getting card at index: " + cardIndexToSwap);
                Card swappedCard = deckService.getCardFromDeck(playerHand.getDeckId(), cardIndexToSwap);
                System.out.println("Got card: " + swappedCard.getValue() + " of " + swappedCard.getSuite());
                message += "Swapped out: " + swappedCard.getValue() + " of " + swappedCard.getSuite() + ". ";

                // Move swapped card to open table
                deckService.removeDeckCards(playerHand.getDeckId(), swappedCard.getId());
                deckService.addCardToDeck(openTableDeck.getDeckId(), swappedCard);

                // Add drawn card to player hand
                deckService.addCardToDeck(playerHand.getDeckId(), drawnCard);
            } else {
                message += "Invalid swap index! Card discarded. ";
                deckService.addCardToDeck(openTableDeck.getDeckId(), drawnCard);
            }
        } else {
            message += "Card discarded to open table. ";
            deckService.addCardToDeck(openTableDeck.getDeckId(), drawnCard);
        }

        // Computer's turn
        if (!gameState.isGameOver()) {
            message += executeComputerTurn(gameState);
        }

        // Check if game should end
        checkGameEnd(gameState);

        // Increment round number only if game is not over
        if (!gameState.isGameOver()) {
            gameState.setRoundNumber(gameState.getRoundNumber() + 1);
        }
        gameStateRepo.save(gameState);

        // Refresh game state for response
        gameState = gameStateRepo.findById(gameId).orElseThrow();

        return buildResponse(gameState, drawnCard, message);

    }

    /**
     * Get current game state without making any moves
     */
    public GameStateResponse getGameStateResponse(Long gameId) {
        GameState gameState = gameStateRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found with id: " + gameId));
        return buildResponse(gameState, null, "Current game state");
    }

    /**
     * End the game and clean up
     */
    @Transactional
    public void endGame(Long gameId) {
        gameStateRepo.deleteById(gameId);
    }

    @Transactional
    public GameStateResponse endGameManually(Long gameId) {
        GameState gameState = gameStateRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found with id: " + gameId));

        gameState.setGameOver(true);
        calculateFinalScores(gameState);
        gameStateRepo.save(gameState);

        return buildResponse(gameState, null, "Game ended manually.");
    }

    /**
     * Build the response DTO with current game state
     */
    private GameStateResponse buildResponse(GameState gameState, Card drawnCard, String message) {
        Deck playerHand = deckService.getDeck(gameState.getPlayerHand().getDeckId());
        Deck computerHand = deckService.getDeck(gameState.getComputerHand().getDeckId());
        Deck mainDeck = deckService.getDeck(gameState.getMainDeck().getDeckId());
        Deck openTableDeck = deckService.getDeck(gameState.getOpenTableDeck().getDeckId());

        /* DEBUG
        System.out.println("Player hand cardIds: " + playerHand.getCardIds().size());
        System.out.println("Player hand cards: " + playerHand.getCards().size());
*/

        // Calculate current scores
        int playerScore = cardService.calculateHandScore(playerHand.getCards());
        int computerScore = cardService.calculateHandScore(computerHand.getCards());

        // Get top card from open table (last card in the list)
        Card topOpenTableCard = null;
        if (!openTableDeck.getCardIds().isEmpty()) {
            Long topCardId = openTableDeck.getCardIds().get(openTableDeck.getCardIds().size() - 1);
            topOpenTableCard = cardService.getCardById(topCardId);
        }

        // Add winner information to message if game is over
        if (gameState.isGameOver()) {
            message += "\n--- GAME OVER ---\n";
            message += "Final Scores: Player = " + playerScore + ", Computer = " + computerScore + "\n";
            if (playerScore < computerScore) {
                message += "🎉 Player wins!";
            } else if (computerScore < playerScore) {
                message += "💻 Computer wins!";
            } else {
                message += "🤝 It's a tie!";
            }
        }

        GameStateResponse response = new GameStateResponse();
        response.setGameId(gameState.getGameId());
        response.setPlayerHand(playerHand.getCards());
        response.setComputerHandSize(computerHand.getCards().size());
        response.setDrawnCard(drawnCard);
        response.setTopOpenTableCard(topOpenTableCard);
        response.setMainDeckSize(mainDeck.getCardIds().size());
        response.setOpenTableSize(openTableDeck.getCardIds().size());
        response.setRoundNumber(gameState.getRoundNumber());
        response.setGameOver(gameState.isGameOver());
        response.setMessage(message);
        response.setPlayerScore(playerScore);
        response.setComputerScore(computerScore);

        /* DEBUG
        System.out.println("Response playerHand size: " + response.getPlayerHand().size());
*/
        return response;
    }

    public void sendGameResultToQueue(String playerName, int playerScore, int computerScore, int rounds) {
        GameResultMessage message = new GameResultMessage(playerName, playerScore, computerScore, rounds);
        rabbitTemplate.convertAndSend(RabbitMQConfig.GAME_RESULT_QUEUE, message);
    }
    public void saveGameResultViaQueue(Long gameId, String playerName) {
        GameState gameState = gameStateRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        sendGameResultToQueue(
                playerName,
                gameState.getPlayerScore(),
                gameState.getComputerScore(),
                gameState.getRoundNumber()
        );
    }

    /**
     * Clone a deck with all its card IDs
     */
    private Deck cloneDeck(Deck originalDeck) {
        Deck clonedDeck = new Deck();
        clonedDeck.setDeckName(originalDeck.getDeckName());
        clonedDeck.setCardIds(new ArrayList<>(originalDeck.getCardIds()));
        return deckService.createDeck(clonedDeck.getDeckName(), clonedDeck.getCardIds());
    }

    /**
     * Clone a complete game state for saving
     */
    private GameState cloneGameState(GameState original) {
        // Clone all decks
        Deck clonedMainDeck = cloneDeck(deckService.getDeck(original.getMainDeck().getDeckId()));
        Deck clonedOpenTableDeck = cloneDeck(deckService.getDeck(original.getOpenTableDeck().getDeckId()));
        Deck clonedPlayerHand = cloneDeck(deckService.getDeck(original.getPlayerHand().getDeckId()));
        Deck clonedComputerHand = cloneDeck(deckService.getDeck(original.getComputerHand().getDeckId()));

        // Create new game state with cloned decks
        GameState clonedState = new GameState();
        clonedState.setMainDeck(clonedMainDeck);
        clonedState.setOpenTableDeck(clonedOpenTableDeck);
        clonedState.setPlayerHand(clonedPlayerHand);
        clonedState.setComputerHand(clonedComputerHand);
        clonedState.setPlayerScore(original.getPlayerScore());
        clonedState.setComputerScore(original.getComputerScore());
        clonedState.setRoundNumber(original.getRoundNumber());
        clonedState.setGameOver(original.isGameOver());

        return gameStateRepo.save(clonedState);
    }

    /**
     * Save current game state for later
     */
    @Transactional
    public SavedGame saveGame(Long gameId, String playerName, String saveName) {
        GameState originalGame = gameStateRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found with id: " + gameId));

        if (originalGame.isGameOver()) {
            throw new RuntimeException("Cannot save a game that is already over!");
        }

        // Clone the game state
        GameState clonedGameState = cloneGameState(originalGame);

        // Create saved game entry
        SavedGame savedGame = new SavedGame();
        savedGame.setPlayerName(playerName);
        savedGame.setSaveName(saveName != null && !saveName.trim().isEmpty() ? saveName : "Saved Game");
        savedGame.setSavedAt(LocalDateTime.now());
        savedGame.setGameState(clonedGameState);

        return savedGameRepository.save(savedGame);
    }

    /**
     * Load a saved game and return it as an active game
     */
    @Transactional
    public GameState loadSavedGame(Long savedGameId) {
        SavedGame savedGame = savedGameRepository.findById(savedGameId)
                .orElseThrow(() -> new RuntimeException("Saved game not found with id: " + savedGameId));

        // Clone the saved game state to create a new active game
        GameState activeGame = cloneGameState(savedGame.getGameState());

        return activeGame;
    }

    /**
     * Get all saved games for a specific player
     */
    public List<SavedGame> getSavedGames(String playerName) {
        if (playerName != null && !playerName.trim().isEmpty()) {
            return savedGameRepository.findByPlayerNameOrderBySavedAtDesc(playerName);
        }
        return savedGameRepository.findAllByOrderBySavedAtDesc();
    }

    /**
     * Delete a saved game
     */
    @Transactional
    public void deleteSavedGame(Long savedGameId) {
        SavedGame savedGame = savedGameRepository.findById(savedGameId)
                .orElseThrow(() -> new RuntimeException("Saved game not found with id: " + savedGameId));

        // Delete the associated game state and decks
        GameState gameState = savedGame.getGameState();
        if (gameState != null) {
            savedGameRepository.delete(savedGame);
            gameStateRepo.delete(gameState);
        } else {
            savedGameRepository.delete(savedGame);
        }
    }
}