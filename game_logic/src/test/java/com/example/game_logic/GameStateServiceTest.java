package com.example.game_logic;

import com.example.game_logic.card.Card;
import com.example.game_logic.card.CardService;
import com.example.game_logic.card.Suite;
import com.example.game_logic.config.RabbitMQConfig;
import com.example.game_logic.decks.Deck;
import com.example.game_logic.decks.DeckService;
import com.example.game_logic.gamestate.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameStateServiceTest {

    @Mock
    private CardService cardService;

    @Mock
    private DeckService deckService;

    @Mock
    private GameStateRepo gameStateRepo;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private GameStateService gameStateService;

    private GameState testGameState;
    private Deck mainDeck;
    private Deck playerHand;
    private Deck computerHand;
    private Deck openTableDeck;

    @BeforeEach
    void setUp() {
        // Setup test decks
        mainDeck = createDeck(1L, "mainDeck", Arrays.asList(1L, 2L, 3L, 4L, 5L));
        playerHand = createDeck(2L, "playerHand", Arrays.asList(10L, 11L, 12L, 13L));
        computerHand = createDeck(3L, "computerHand", Arrays.asList(20L, 21L, 22L, 23L));
        // Start with an empty open table deck - tests can add cards as needed
        openTableDeck = createDeck(4L, "openTableDeck", new ArrayList<>());

        // Setup test game state
        testGameState = new GameState();
        testGameState.setGameId(1L);
        testGameState.setMainDeck(mainDeck);
        testGameState.setPlayerHand(playerHand);
        testGameState.setComputerHand(computerHand);
        testGameState.setOpenTableDeck(openTableDeck);
        testGameState.setRoundNumber(1);
        testGameState.setGameOver(false);
        testGameState.setPlayerScore(0);
        testGameState.setComputerScore(0);
    }

    private Deck createDeck(Long id, String name, List<Long> cardIds) {
        Deck deck = new Deck();
        deck.setDeckId(id);
        deck.setDeckName(name);
        deck.setCardIds(new ArrayList<>(cardIds));

        // Create corresponding Card objects
        List<Card> cards = new ArrayList<>();
        for (Long cardId : cardIds) {
            Card card = new Card();
            card.setId(cardId);
            card.setValue((int) (cardId % 13) + 1);
            card.setSuite(Suite.SPADES);
            cards.add(card);
        }
        deck.setCards(cards);

        return deck;
    }

    private Card createCard(Long id, int value, Suite suite) {
        Card card = new Card();
        card.setId(id);
        card.setValue(value);
        card.setSuite(suite);
        card.setFilename(value + "_of_" + suite.name().toLowerCase() + ".svg");
        return card;
    }

    @Test
    void initializeGame_ShouldCreateNewGameWithShuffledDeck() {
        // Arrange
        Deck createdMainDeck = createDeck(10L, "mainDeck", new ArrayList<>());
        Deck createdPlayerHand = createDeck(11L, "playerHand", Arrays.asList(1L, 2L, 3L, 4L));
        Deck createdComputerHand = createDeck(12L, "computerHand", Arrays.asList(5L, 6L, 7L, 8L));
        Deck createdOpenTable = createDeck(13L, "openTableDeck", new ArrayList<>());

        when(deckService.createDeck(eq("mainDeck"), anyList())).thenReturn(createdMainDeck);
        when(deckService.drawCards(eq(10L), eq(4)))
                .thenReturn(Arrays.asList(1L, 2L, 3L, 4L))
                .thenReturn(Arrays.asList(5L, 6L, 7L, 8L));
        when(deckService.createDeck(eq("playerHand"), anyList())).thenReturn(createdPlayerHand);
        when(deckService.createDeck(eq("computerHand"), anyList())).thenReturn(createdComputerHand);
        when(deckService.createDeck(eq("openTableDeck"), anyList())).thenReturn(createdOpenTable);

        GameState savedGameState = new GameState();
        savedGameState.setGameId(1L);
        when(gameStateRepo.save(any(GameState.class))).thenReturn(savedGameState);

        // Act
        GameState result = gameStateService.initializeGame();

        // Assert
        assertNotNull(result);
        verify(deckService).createDeck(eq("mainDeck"), anyList());
        verify(deckService).shuffleDeck(10L);
        verify(deckService, times(2)).drawCards(eq(10L), eq(4));
        verify(deckService).createDeck(eq("playerHand"), anyList());
        verify(deckService).createDeck(eq("computerHand"), anyList());
        verify(deckService).createDeck(eq("openTableDeck"), anyList());
        verify(gameStateRepo).save(any(GameState.class));
    }

    @Test
    void drawCard_FromMainDeck_ShouldReturnTopCard() {
        // Arrange
        Long gameId = 1L;
        Card expectedCard = createCard(1L, 7, Suite.HEARTS);

        when(gameStateRepo.findById(gameId)).thenReturn(Optional.of(testGameState));
        when(deckService.getDeck(mainDeck.getDeckId())).thenReturn(mainDeck);
        when(deckService.getDeck(openTableDeck.getDeckId())).thenReturn(openTableDeck);
        when(cardService.getCardById(1L)).thenReturn(expectedCard);

        // Act
        Card result = gameStateService.drawCard(gameId, "mainDeck");

        // Assert
        assertNotNull(result);
        assertEquals(expectedCard.getId(), result.getId());
        verify(cardService).getCardById(1L);
        verify(deckService, never()).removeDeckCards(anyLong(), anyLong());
    }

    @Test
    void drawCard_FromOpenTable_ShouldReturnTopCard() {
        // Arrange
        Long gameId = 1L;
        openTableDeck.getCardIds().add(50L);
        Card expectedCard = createCard(50L, 9, Suite.DIAMONDS);

        when(gameStateRepo.findById(gameId)).thenReturn(Optional.of(testGameState));
        when(deckService.getDeck(mainDeck.getDeckId())).thenReturn(mainDeck);
        when(deckService.getDeck(openTableDeck.getDeckId())).thenReturn(openTableDeck);
        when(cardService.getCardById(50L)).thenReturn(expectedCard);

        // Act
        Card result = gameStateService.drawCard(gameId, "openTable");

        // Assert
        assertNotNull(result);
        assertEquals(expectedCard.getId(), result.getId());
        verify(cardService).getCardById(50L);
    }

    @Test
    void drawCard_WhenGameNotFound_ShouldThrowException() {
        // Arrange
        Long gameId = 999L;
        when(gameStateRepo.findById(gameId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> gameStateService.drawCard(gameId, "mainDeck"));

        assertTrue(exception.getMessage().contains("Game not found"));
    }

    @Test
    void drawCard_WhenGameIsOver_ShouldThrowException() {
        // Arrange
        testGameState.setGameOver(true);
        when(gameStateRepo.findById(1L)).thenReturn(Optional.of(testGameState));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> gameStateService.drawCard(1L, "mainDeck"));

        assertTrue(exception.getMessage().contains("Game is already over"));
    }

    @Test
    void completeTurn_PlayerSwapsCard_ShouldSwapCorrectly() {
        // Arrange
        Long gameId = 1L;
        Card drawnCard = createCard(30L, 5, Suite.HEARTS);
        Card cardToSwap = playerHand.getCards().get(0);
        Card computerDrawnCard = createCard(40L, 8, Suite.CLUBS);
        int swapIndex = 0;

        when(gameStateRepo.findById(gameId))
                .thenReturn(Optional.of(testGameState))
                .thenReturn(Optional.of(testGameState));
        when(deckService.getDeck(playerHand.getDeckId())).thenReturn(playerHand);
        when(deckService.getDeck(computerHand.getDeckId())).thenReturn(computerHand);
        when(deckService.getDeck(mainDeck.getDeckId())).thenReturn(mainDeck);
        when(deckService.getDeck(openTableDeck.getDeckId())).thenReturn(openTableDeck);
        when(deckService.getCardFromDeck(playerHand.getDeckId(), swapIndex)).thenReturn(cardToSwap);

        // Computer will also draw a card - use anyLong to match any deck ID
        when(deckService.drawCards(anyLong(), eq(1)))
                .thenReturn(Arrays.asList(40L));
        // Stub getCardById for any card the computer might check or draw
        when(cardService.getCardById(anyLong())).thenReturn(computerDrawnCard);
        when(cardService.calculateCardScore(any(Card.class))).thenReturn(5, 8, 10);
        when(cardService.calculateHandScore(anyList())).thenReturn(15);
        when(gameStateRepo.save(any(GameState.class))).thenReturn(testGameState);

        // Act
        GameStateResponse response = gameStateService.completeTurn(
                gameId, drawnCard, true, swapIndex, "mainDeck");

        // Assert
        assertNotNull(response);
        // Should be called at least once for player and computer
        verify(deckService, atLeastOnce()).drawCards(anyLong(), eq(1));
        verify(deckService).removeDeckCards(playerHand.getDeckId(), cardToSwap.getId());
        verify(deckService).addCardToDeck(openTableDeck.getDeckId(), cardToSwap);
        verify(deckService).addCardToDeck(playerHand.getDeckId(), drawnCard);
        verify(gameStateRepo).save(any(GameState.class));
    }

    @Test
    void completeTurn_PlayerDiscardsCard_ShouldAddToOpenTable() {
        // Arrange
        Long gameId = 1L;
        Card drawnCard = createCard(30L, 10, Suite.CLUBS);
        Card computerDrawnCard = createCard(40L, 6, Suite.HEARTS);

        when(gameStateRepo.findById(gameId))
                .thenReturn(Optional.of(testGameState))
                .thenReturn(Optional.of(testGameState));
        when(deckService.getDeck(playerHand.getDeckId())).thenReturn(playerHand);
        when(deckService.getDeck(computerHand.getDeckId())).thenReturn(computerHand);
        when(deckService.getDeck(mainDeck.getDeckId())).thenReturn(mainDeck);
        when(deckService.getDeck(openTableDeck.getDeckId())).thenReturn(openTableDeck);

        // Mock computer's draw - use anyLong
        when(deckService.drawCards(anyLong(), eq(1)))
                .thenReturn(Arrays.asList(40L));
        // Stub for any card ID
        when(cardService.getCardById(anyLong())).thenReturn(computerDrawnCard);
        when(cardService.calculateCardScore(any(Card.class))).thenReturn(10, 6, 8);
        when(cardService.calculateHandScore(anyList())).thenReturn(15);
        when(gameStateRepo.save(any(GameState.class))).thenReturn(testGameState);

        // Act
        GameStateResponse response = gameStateService.completeTurn(
                gameId, drawnCard, false, null, "mainDeck");

        // Assert
        assertNotNull(response);
        assertTrue(response.getMessage().contains("discarded"));
        verify(deckService).addCardToDeck(openTableDeck.getDeckId(), drawnCard);
        verify(deckService, never()).removeDeckCards(eq(playerHand.getDeckId()), anyLong());
    }

    @Test
    void completeTurn_ShouldIncrementRoundNumber() {
        // Arrange
        Long gameId = 1L;
        Card drawnCard = createCard(30L, 5, Suite.HEARTS);
        Card computerDrawnCard = createCard(40L, 6, Suite.CLUBS);
        int initialRound = testGameState.getRoundNumber();

        when(gameStateRepo.findById(gameId))
                .thenReturn(Optional.of(testGameState))
                .thenReturn(Optional.of(testGameState));
        when(deckService.getDeck(playerHand.getDeckId())).thenReturn(playerHand);
        when(deckService.getDeck(computerHand.getDeckId())).thenReturn(computerHand);
        when(deckService.getDeck(mainDeck.getDeckId())).thenReturn(mainDeck);
        when(deckService.getDeck(openTableDeck.getDeckId())).thenReturn(openTableDeck);

        // Mock computer's draw - use anyLong() to handle any deck ID
        when(deckService.drawCards(anyLong(), eq(1)))
                .thenReturn(Arrays.asList(40L));
        // Stub for any card ID
        when(cardService.getCardById(anyLong())).thenReturn(computerDrawnCard);
        when(cardService.calculateCardScore(any(Card.class))).thenReturn(5, 6, 8);
        when(cardService.calculateHandScore(anyList())).thenReturn(15);
        when(gameStateRepo.save(any(GameState.class))).thenReturn(testGameState);

        // Act
        gameStateService.completeTurn(gameId, drawnCard, false, null, "mainDeck");

        // Assert
        ArgumentCaptor<GameState> gameStateCaptor = ArgumentCaptor.forClass(GameState.class);
        verify(gameStateRepo).save(gameStateCaptor.capture());
        GameState savedState = gameStateCaptor.getValue();
        assertEquals(initialRound + 1, savedState.getRoundNumber());
    }

    @Test
    void completeTurn_WhenDeckEmpty_ShouldEndGame() {
        // Arrange
        Long gameId = 1L;
        Card drawnCard = createCard(30L, 5, Suite.HEARTS);
        mainDeck.getCardIds().clear(); // Empty deck

        when(gameStateRepo.findById(gameId))
                .thenReturn(Optional.of(testGameState))
                .thenReturn(Optional.of(testGameState));
        when(deckService.getDeck(mainDeck.getDeckId())).thenReturn(mainDeck);
        when(deckService.getDeck(playerHand.getDeckId())).thenReturn(playerHand);
        when(deckService.getDeck(computerHand.getDeckId())).thenReturn(computerHand);
        when(deckService.getDeck(openTableDeck.getDeckId())).thenReturn(openTableDeck);
        when(cardService.calculateHandScore(anyList())).thenReturn(15, 20);
        when(gameStateRepo.save(any(GameState.class))).thenReturn(testGameState);

        // Act
        GameStateResponse response = gameStateService.completeTurn(
                gameId, drawnCard, false, null, "mainDeck");

        // Assert
        assertTrue(response.isGameOver());
        assertTrue(response.getMessage().contains("GAME OVER"));
    }

    @Test
    void getGameStateResponse_ShouldReturnCurrentState() {
        // Arrange
        Long gameId = 1L;
        when(gameStateRepo.findById(gameId)).thenReturn(Optional.of(testGameState));
        when(deckService.getDeck(playerHand.getDeckId())).thenReturn(playerHand);
        when(deckService.getDeck(computerHand.getDeckId())).thenReturn(computerHand);
        when(deckService.getDeck(mainDeck.getDeckId())).thenReturn(mainDeck);
        when(deckService.getDeck(openTableDeck.getDeckId())).thenReturn(openTableDeck);
        when(cardService.calculateHandScore(anyList())).thenReturn(10, 12);

        // Act
        GameStateResponse response = gameStateService.getGameStateResponse(gameId);

        // Assert
        assertNotNull(response);
        assertEquals(gameId, response.getGameId());
        assertEquals(playerHand.getCards().size(), response.getPlayerHand().size());
        assertEquals(computerHand.getCards().size(), response.getComputerHandSize());
        assertEquals(1, response.getRoundNumber());
        assertFalse(response.isGameOver());
    }

    @Test
    void endGameManually_ShouldSetGameOverAndCalculateScores() {
        // Arrange
        Long gameId = 1L;
        when(gameStateRepo.findById(gameId)).thenReturn(Optional.of(testGameState));
        when(deckService.getDeck(playerHand.getDeckId())).thenReturn(playerHand);
        when(deckService.getDeck(computerHand.getDeckId())).thenReturn(computerHand);
        when(deckService.getDeck(mainDeck.getDeckId())).thenReturn(mainDeck);
        when(deckService.getDeck(openTableDeck.getDeckId())).thenReturn(openTableDeck);
        when(cardService.calculateHandScore(anyList())).thenReturn(15, 20);
        when(gameStateRepo.save(any(GameState.class))).thenReturn(testGameState);

        // Act
        GameStateResponse response = gameStateService.endGameManually(gameId);

        // Assert
        assertTrue(response.isGameOver());
        assertTrue(response.getMessage().contains("ended manually"));
        ArgumentCaptor<GameState> captor = ArgumentCaptor.forClass(GameState.class);
        verify(gameStateRepo).save(captor.capture());
        assertTrue(captor.getValue().isGameOver());
    }

    @Test
    void saveGameResultViaQueue_ShouldSendMessageToRabbitMQ() {
        // Arrange
        Long gameId = 1L;
        String playerName = "TestPlayer";
        testGameState.setPlayerScore(15);
        testGameState.setComputerScore(20);
        testGameState.setRoundNumber(5);

        when(gameStateRepo.findById(gameId)).thenReturn(Optional.of(testGameState));

        // Act
        gameStateService.saveGameResultViaQueue(gameId, playerName);

        // Assert
        ArgumentCaptor<GameResultMessage> messageCaptor = ArgumentCaptor.forClass(GameResultMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.GAME_RESULT_QUEUE),
                messageCaptor.capture()
        );

        GameResultMessage sentMessage = messageCaptor.getValue();
        assertEquals(playerName, sentMessage.getPlayerName());
        assertEquals(15, sentMessage.getPlayerScore());
        assertEquals(20, sentMessage.getComputerScore());
        assertEquals(5, sentMessage.getRounds());
    }

    @Test
    void completeTurn_InvalidSwapIndex_ShouldDiscardCard() {
        // Arrange
        Long gameId = 1L;
        Card drawnCard = createCard(30L, 5, Suite.HEARTS);
        Card computerDrawnCard = createCard(40L, 7, Suite.DIAMONDS);
        int invalidIndex = 999;

        when(gameStateRepo.findById(gameId))
                .thenReturn(Optional.of(testGameState))
                .thenReturn(Optional.of(testGameState));
        when(deckService.getDeck(playerHand.getDeckId())).thenReturn(playerHand);
        when(deckService.getDeck(computerHand.getDeckId())).thenReturn(computerHand);
        when(deckService.getDeck(mainDeck.getDeckId())).thenReturn(mainDeck);
        when(deckService.getDeck(openTableDeck.getDeckId())).thenReturn(openTableDeck);

        // Mock computer's draw - use anyLong
        when(deckService.drawCards(anyLong(), eq(1)))
                .thenReturn(Arrays.asList(40L));
        // Stub for any card ID
        when(cardService.getCardById(anyLong())).thenReturn(computerDrawnCard);
        when(cardService.calculateCardScore(any(Card.class))).thenReturn(5, 7, 9);
        when(cardService.calculateHandScore(anyList())).thenReturn(15);
        when(gameStateRepo.save(any(GameState.class))).thenReturn(testGameState);

        // Act
        GameStateResponse response = gameStateService.completeTurn(
                gameId, drawnCard, true, invalidIndex, "mainDeck");

        // Assert
        assertTrue(response.getMessage().contains("Invalid swap index"));
        verify(deckService).addCardToDeck(openTableDeck.getDeckId(), drawnCard);
    }


    @Test
    void drawCard_WithInvalidDrawSource_ShouldThrowException() {
        // Arrange
        Long gameId = 1L;
        when(gameStateRepo.findById(gameId)).thenReturn(Optional.of(testGameState));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> gameStateService.drawCard(gameId, "invalidSource"));

        assertTrue(exception.getMessage().contains("Invalid draw source"));
    }

    @Test
    void completeTurn_WhenGameOver_ShouldNotIncrementRound() {
        // Arrange
        Long gameId = 1L;
        Card drawnCard = createCard(30L, 5, Suite.HEARTS);
        mainDeck.getCardIds().clear(); // Empty deck to trigger game over
        int initialRound = testGameState.getRoundNumber();

        when(gameStateRepo.findById(gameId))
                .thenReturn(Optional.of(testGameState))
                .thenReturn(Optional.of(testGameState));
        when(deckService.getDeck(mainDeck.getDeckId())).thenReturn(mainDeck);
        when(deckService.getDeck(playerHand.getDeckId())).thenReturn(playerHand);
        when(deckService.getDeck(computerHand.getDeckId())).thenReturn(computerHand);
        when(deckService.getDeck(openTableDeck.getDeckId())).thenReturn(openTableDeck);
        when(cardService.calculateHandScore(anyList())).thenReturn(15, 20);
        when(gameStateRepo.save(any(GameState.class))).thenReturn(testGameState);

        // Act
        gameStateService.completeTurn(gameId, drawnCard, false, null, "mainDeck");

        // Assert
        ArgumentCaptor<GameState> captor = ArgumentCaptor.forClass(GameState.class);
        verify(gameStateRepo).save(captor.capture());
        GameState savedState = captor.getValue();
        assertEquals(initialRound, savedState.getRoundNumber()); // Should not increment when game over
    }
}




