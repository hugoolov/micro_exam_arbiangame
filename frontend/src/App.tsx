import { useState, useEffect, useMemo } from 'react';
import './App.css';

// ==================== INTERFACES ====================
interface WeatherData {
  temperature: number;
  windSpeed: number;
  symbolCode: string;
  condition: string;
  shouldGoOutside: boolean;
  message: string;
}

interface GameResult {
  id: number;
  playerName: string;
  playerScore: number;
  computerScore: number;
  winner: string;
  rounds: number;
  gameDate: string;
}

type Stats = {
  games: number;
  playerWins: number;
  computerWins: number;
  ties: number;
  winRate: number;
  avgRounds: number;
  avgPlayerScore: number;
  avgComputerScore: number;
};

interface Badge {
  id: BadgeId;
  name: string;
  description: string;
  icon: string;
  maxProgress?: number;
  condition: (ctx: { stats: Stats; results: GameResult[] }) => {
    earned: boolean;
    progress?: number;
  };
}

interface EarnedBadge {
  id: BadgeId;
  earnedAt: string;
}

// ==================== BADGDES ==============================
type BadgeId =
    | "first_game"
    | "first_win"
    | "ten_games"
    | "win_rate_50"
    | "flawless_5"
    | "perfect_score";

const BADGES: Badge[] = [
  {
    id: "first_game",
    name: "First Game",
    description: "Play your very first game.",
    icon: "🎬",
    maxProgress: 1,
    condition: ({ stats }) => ({
      earned: stats.games >= 1,
      progress: Math.min(stats.games, 1),
    }),
  },
  {
    id: "first_win",
    name: "First Win",
    description: "Win one game.",
    icon: "🏆",
    maxProgress: 1,
    condition: ({ stats }) => ({
      earned: stats.playerWins >= 1,
      progress: Math.min(stats.playerWins, 1),
    }),
  },
  {
    id: "ten_games",
    name: "Played 10 Games",
    description: "Reach 10 total games played.",
    icon: "🔟",
    maxProgress: 10,
    condition: ({ stats }) => ({
      earned: stats.games >= 10,
      progress: Math.min(stats.games, 10),
    }),
  },
  {
    id: "win_rate_50",
    name: "Winning Record",
    description: "Maintain a win rate of 50% or higher after 5+ games.",
    icon: "📈",
    condition: ({ stats }) => ({
      earned: stats.games >= 5 && stats.winRate >= 0.5,
    }),
  },
  {
    id: "flawless_5",
    name: "Flawless",
    description: "Win 5 games with 0 losses recorded.",
    icon: "💯",
    maxProgress: 5,
    condition: ({ results }) => {
      const wins = results.filter(r => r.winner === "PLAYER").length;
      const losses = results.filter(r => r.winner === "COMPUTER").length;
      return { earned: wins >= 5 && losses === 0, progress: Math.min(wins, 5) };
    },
  },
  {
    id: "perfect_score",
    name: "Perfect score",
    description: "Get the lowest score possible",
    icon: "🔥",
    condition: ({ results }) => ({
      earned: results.some(r => r.playerScore >= 0),
    }),
  },
];

// ==================== WEATHER COMPONENT ====================
const WeatherDisplay: React.FC = () => {
  const [weather, setWeather] = useState<WeatherData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchWeather();
  }, []);

  const fetchWeather = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/weather');
      if (!response.ok) throw new Error('Failed to fetch weather');
      const data = await response.json();
      setWeather(data);
    } catch (err) {
      console.error('Error fetching weather:', err);
      setError('Could not load weather data');
    } finally {
      setLoading(false);
    }
  };

  const getWeatherIcon = (condition: string) => {
    const icons: Record<string, string> = {
      sunny: '☀️',
      partly_cloudy: '⛅',
      cloudy: '☁️',
      rainy: '🌧️',
      snowy: '❄️',
      thunderstorm: '⛈️',
      foggy: '🌫️',
      unavailable: '❓'
    };
    return icons[condition] || '🌤️';
  };

  if (loading) return <div className="weather-display loading">Loading weather...</div>;
  if (error || !weather) return <div className="weather-display error">Weather unavailable</div>;

  return (
    <div className={`weather-display ${weather.shouldGoOutside ? 'sunny-alert' : ''}`}>
      <div className="weather-content">
        <div className="weather-icon">{getWeatherIcon(weather.condition)}</div>
        <div className="weather-info">
          <div className="temperature">{Math.round(weather.temperature)}°C</div>
          <div className="condition">{weather.condition.replace('_', ' ')}</div>
          <div className="wind">Wind: {Math.round(weather.windSpeed)} m/s</div>
        </div>
      </div>

      {weather.shouldGoOutside ? (
        <div className="weather-alert">
          <h3>🌞 Go Outside! 🌞</h3>
          <p>{weather.message}</p>
        </div>
      ) : (
        <div className="weather-message">
          <p>{weather.message}</p>
        </div>
      )}
    </div>
  );
};

// ==================== LOGIN/REGISTER COMPONENT ====================
const AuthPage: React.FC<{ onLogin: (username: string, userId: number) => void; onNotify}> = ({ onLogin, onNotify }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showRegister, setShowRegister] = useState(false);
  const [newUsername, setNewUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [usernameAvailable, setUsernameAvailable] = useState<boolean | null>(null);

  const handleLogin = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      if (response.ok) {
        const data = await response.json();
        alert(`Logged in as ${data.username}`);
        onLogin(data.username, data.userId);
      } else {
        alert(`Login failed insanely: ${await response.text()}`);
      }
    } catch {
      alert('Something went wrong during login');
    }
  };

  const [registerLoading, setRegisterLoading] = useState(false);

  const handleRegister = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: newUsername, password: newPassword }),
      });

      if (!response.ok) {
        alert(`Registration failed: ${await response.text()}`);
        return;
      }

      const data = await response.json(); // expect { username, userId, ... }
      // Auto login if success
      onLogin(data.username, data.userId);
      onNotify(`Welcome, ${data.username}! Your account was created and you are now logged in.`,'success');

      setShowRegister(false);
      setNewUsername('');
      setNewPassword('');
    } catch {
      alert('Something went wrong during registration');
    } finally {
      setRegisterLoading(false);
    }
  };

  const checkUsername = async (username: string) => {
    if (username.length < 3) return setUsernameAvailable(null);
    try {
      const response = await fetch(`http://localhost:8080/api/auth/check-username?username=${username}`);
      const available = await response.json();
      setUsernameAvailable(available);
    } catch (error) {
      console.error('Error checking username:', error);
    }
  };

  return (
    <div className="auth-page">
      <h1>Arabian Card Game</h1>
      <WeatherDisplay />

      <div className="auth-form">
        <h2>Login</h2>
        <input
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && handleLogin()}
        />
        <button onClick={handleLogin}>Login</button>
        <button onClick={() => setShowRegister(true)} className="secondary">Register New Account</button>
      </div>

      {showRegister && (
        <div className="modal">
          <div className="modal-content">
            <h2>Register New User</h2>
            <input
              type="text"
              placeholder="New Username"
              value={newUsername}
              onChange={(e) => {
                setNewUsername(e.target.value);
                checkUsername(e.target.value);
              }}
            />
            {usernameAvailable === false && <p style={{ color: 'red' }}>Username taken</p>}
            {usernameAvailable === true && <p style={{ color: 'green' }}>Username available</p>}
            <input
              type="password"
              placeholder="New Password (min 6 characters)"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />
            <div className="modal-buttons">
              <button onClick={handleRegister}>Register</button>
              <button onClick={() => setShowRegister(false)} className="secondary">Cancel</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// ==================== SAVED GAME INTERFACES ====================
interface SavedGame {
  id: number;
  playerName: string;
  saveName: string;
  savedAt: string;
  roundNumber: number;
  playerScore: number;
  computerScore: number;
  gameStateId: number;
}

// ==================== GAME COMPONENT ====================
const GamePage: React.FC<{ username: string }> = ({ username }) => {
  const [gameState, setGameState] = useState<any>(null);
  const [drawnCard, setDrawnCard] = useState<any>(null);
  const [selectedCardIndex, setSelectedCardIndex] = useState<number | null>(null);
  const [drawFrom, setDrawFrom] = useState<string | null>(null);
  const [showSaveModal, setShowSaveModal] = useState(false);
  const [saveName, setSaveName] = useState('');
  const [showLoadModal, setShowLoadModal] = useState(false);
  const [savedGames, setSavedGames] = useState<SavedGame[]>([]);

  const startGame = async () => {
    const response = await fetch('http://localhost:8080/api/game/start', {
      method: 'POST'
    });
    const data = await response.json();
    setGameState(data);
    setDrawnCard(null);
    setSelectedCardIndex(null);
  };

  const drawCard = async (source: 'mainDeck' | 'openTable') => {
    const response = await fetch(`http://localhost:8080/api/game/${gameState.gameId}/draw?from=${source}`, {
      method: 'POST'
    });
    const card = await response.json();
    setDrawnCard(card);
    setDrawFrom(source);
  };

  const swapCard = async () => {
    if (selectedCardIndex === null) {
      alert('Please select a card from your hand to swap');
      return;
    }

    const response = await fetch(`http://localhost:8080/api/game/${gameState.gameId}/complete-turn`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        drawnCard: drawnCard,
        swap: true,
        cardIndexToSwap: selectedCardIndex,
        drawFrom: drawFrom
      })
    });
    const data = await response.json();
    setGameState(data);
    setDrawnCard(null);
    setSelectedCardIndex(null);
    setDrawFrom(null);
  };

  const discardCard = async () => {
    const response = await fetch(`http://localhost:8080/api/game/${gameState.gameId}/complete-turn`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        drawnCard: drawnCard,
        swap: false,
        cardIndexToSwap: null,
        drawFrom: drawFrom
      })
    });
    const data = await response.json();
    setGameState(data);
    setDrawnCard(null);
    setSelectedCardIndex(null);
    setDrawFrom(null);
  };

  const endGame = async () => {
    const response = await fetch(`http://localhost:8080/api/game/${gameState.gameId}/end`, {
      method: 'POST'
    });
    const data = await response.json();
    setGameState(data);
  };

  const saveGameResult = async () => {
    await fetch(`http://localhost:8080/api/game/${gameState.gameId}/save-result`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerName: username })
    });
    alert('Result saved!');
  };

  const saveGame = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/game/${gameState.gameId}/save`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          playerName: username,
          saveName: saveName || 'Saved Game'
        })
      });

      if (response.ok) {
        alert('Game saved successfully!');
        setShowSaveModal(false);
        setSaveName('');
      } else {
        alert('Failed to save game');
      }
    } catch (error) {
      console.error('Error saving game:', error);
      alert('Error saving game');
    }
  };

  const loadSavedGames = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/game/saved?playerName=${username}`);
      const data = await response.json();
      setSavedGames(data);
      setShowLoadModal(true);
    } catch (error) {
      console.error('Error loading saved games:', error);
      alert('Error loading saved games');
    }
  };

  const loadGame = async (savedGameId: number) => {
    try {
      const response = await fetch(`http://localhost:8080/api/game/load/${savedGameId}`, {
        method: 'POST'
      });
      const data = await response.json();
      setGameState(data);
      setDrawnCard(null);
      setSelectedCardIndex(null);
      setShowLoadModal(false);
      alert('Game loaded successfully!');
    } catch (error) {
      console.error('Error loading game:', error);
      alert('Error loading game');
    }
  };

  const deleteSavedGame = async (savedGameId: number) => {
    if (!confirm('Are you sure you want to delete this saved game?')) {
      return;
    }

    try {
      const response = await fetch(`http://localhost:8080/api/game/saved/${savedGameId}`, {
        method: 'DELETE'
      });

      if (response.ok) {
        setSavedGames(savedGames.filter(g => g.id !== savedGameId));
        alert('Saved game deleted');
      }
    } catch (error) {
      console.error('Error deleting saved game:', error);
      alert('Error deleting saved game');
    }
  };

  return (
    <div className="game-page">
      <h1>Card Game</h1>
      <p>Playing as: <strong>{username}</strong></p>

      {!gameState && (
          <div className="game-landing">
            <section className="howto-card">
              <h2>How to Play</h2>
              <ol>
                <li>Both you and your opponent get dealt a set of cards.</li>
                <li>Draw a card from the Main Deck or the Open Table.</li>
                <li>Either swap it with a card in your hand, or discard it.</li>
                <li>Note that if you discard a card, your opponent will get a chance to pick it up in the next round</li>
                <li>Try to finish with the lowest total score.</li>
                <li>End the game when you think your hand is best.</li>
                <li>All aces have score of -5p.</li>
                <li>Ten of Hearts and Ten of Diamonds are both -10p.</li>
                <li>All the rest of the cards have their own value.</li>
              </ol>
              <p className="hint">Tip: Watch the discard pile to plan smarter swaps.</p>
            </section>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
              <button onClick={startGame} className="start-game-btn start-game-btn--landing">
                Start New Game
              </button>
              <button onClick={loadSavedGames} className="start-game-btn start-game-btn--landing">
                Load Saved Game
              </button>
            </div>
          </div>
      )}

      {gameState && (
        <div className="game-container">
          <div className="game-controls">
            <button onClick={() => drawCard('mainDeck')} disabled={drawnCard || gameState.gameOver}>
              Draw from Main Deck ({gameState.mainDeckSize} cards)
            </button>
            <button
              onClick={() => drawCard('openTable')}
              disabled={!gameState.topOpenTableCard || drawnCard || gameState.gameOver}
            >
              Draw from Open Table
            </button>
            {!gameState.gameOver && (
              <>
                <button onClick={endGame} className="secondary">End Game Early</button>
                <button onClick={() => setShowSaveModal(true)} className="secondary">Save Game</button>
              </>
            )}
          </div>

          {drawnCard && (
            <div className="drawn-card-section">
              <h3>Drawn Card</h3>
              <img
                src={`/card_svg/${drawnCard.filename}`}
                alt={`${drawnCard.value} of ${drawnCard.suite}`}
                className="card-image"
              />
              <p>Select a card from your hand to swap, or discard</p>
              <div>
                <button onClick={swapCard}>Swap with Selected Card</button>
                <button onClick={discardCard} className="secondary">Discard</button>
              </div>
            </div>
          )}

          {gameState.gameOver && (
            <div className="game-over">
              <h2>🎉 Game Over!</h2>
              <p className="final-scores">
                Final Scores: You: {gameState.playerScore} | Computer: {gameState.computerScore}
              </p>
              <p className="winner-message">{gameState.message}</p>
              <div className="game-over-buttons">
                <button onClick={saveGameResult}>Save Result</button>
                <button onClick={startGame}>Start New Game</button>
              </div>
            </div>
          )}

          <div className="game-board">
            <div className="player-section">
              <h2>Your Hand (Click to select)</h2>
              <div className="card-hand">
                {gameState.playerHand.map((card: any, index: number) => (
                  <img
                    key={card.id}
                    src={`/card_svg/${card.filename}`}
                    alt={`${card.value} of ${card.suite}`}
                    className={`card-image ${selectedCardIndex === index ? 'selected' : ''}`}
                    onClick={() => !gameState.gameOver && setSelectedCardIndex(index)}
                  />
                ))}
              </div>
            </div>

            <div className="computer-section">
              <h2>Computer Hand</h2>
              <div className="card-hand">
                {[...Array(gameState.computerHandSize)].map((_, i) => (
                  <div key={i} className="card-back" />
                ))}
              </div>
            </div>

            <div className="table-section">
              <h2>Open Table (Discard Pile)</h2>
              {gameState.topOpenTableCard ? (
                <img
                  src={`/card_svg/${gameState.topOpenTableCard.filename}`}
                  alt={`${gameState.topOpenTableCard.value} of ${gameState.topOpenTableCard.suite}`}
                  className="card-image"
                />
              ) : (
                <p>Empty</p>
              )}
            </div>
          </div>

          <div className="game-info">
            <p><strong>Round:</strong> {gameState.roundNumber}</p>
            <p><strong>Scores:</strong> You: {gameState.playerScore} | Computer: {gameState.computerScore}</p>
            {gameState.message && <p className="game-message">{gameState.message}</p>}
          </div>
        </div>
      )}

      {/* Save Game Modal */}
      {showSaveModal && (
        <div className="modal-overlay" onClick={() => setShowSaveModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Save Game</h2>
            <p>Enter a name for this saved game:</p>
            <input
              type="text"
              placeholder="Game name (optional)"
              value={saveName}
              onChange={(e) => setSaveName(e.target.value)}
              autoFocus
            />
            <div className="modal-buttons">
              <button onClick={saveGame}>Save</button>
              <button onClick={() => setShowSaveModal(false)} className="secondary">Cancel</button>
            </div>
          </div>
        </div>
      )}

      {/* Load Game Modal */}
      {showLoadModal && (
        <div className="modal-overlay" onClick={() => setShowLoadModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '600px' }}>
            <h2>Load Saved Game</h2>
            {savedGames.length === 0 ? (
              <p>No saved games found.</p>
            ) : (
              <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ borderBottom: '2px solid #ccc' }}>
                      <th style={{ padding: '8px', textAlign: 'left' }}>Name</th>
                      <th style={{ padding: '8px', textAlign: 'center' }}>Round</th>
                      <th style={{ padding: '8px', textAlign: 'center' }}>Score</th>
                      <th style={{ padding: '8px', textAlign: 'center' }}>Saved</th>
                      <th style={{ padding: '8px', textAlign: 'center' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {savedGames.map((game) => (
                      <tr key={game.id} style={{ borderBottom: '1px solid #eee' }}>
                        <td style={{ padding: '8px' }}>{game.saveName}</td>
                        <td style={{ padding: '8px', textAlign: 'center' }}>{game.roundNumber}</td>
                        <td style={{ padding: '8px', textAlign: 'center' }}>
                          {game.playerScore} - {game.computerScore}
                        </td>
                        <td style={{ padding: '8px', textAlign: 'center' }}>
                          {new Date(game.savedAt).toLocaleDateString()}
                        </td>
                        <td style={{ padding: '8px', textAlign: 'center' }}>
                          <button
                            onClick={() => loadGame(game.id)}
                            style={{ marginRight: '5px', padding: '4px 8px' }}
                          >
                            Load
                          </button>
                          <button
                            onClick={() => deleteSavedGame(game.id)}
                            className="secondary"
                            style={{ padding: '4px 8px' }}
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <div className="modal-buttons" style={{ marginTop: '20px' }}>
              <button onClick={() => setShowLoadModal(false)} className="secondary">Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// ==================== RESULTS COMPONENT ====================
const ResultsPage: React.FC = () => {
  const [results, setResults] = useState<GameResult[]>([]);
  const [loading, setLoading] = useState(true);

  // Filters
  const [search, setSearch] = useState("");
  const [winnerFilter, setWinnerFilter] = useState<"" | "PLAYER" | "COMPUTER" | "TIE">("");
  const [fromDate, setFromDate] = useState<string>(""); // yyyy-mm-dd
  const [toDate, setToDate] = useState<string>("");

  const [sortKey, setSortKey] = useState<keyof GameResult>("gameDate");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("desc");

  useEffect(() => {
    fetchResults();
  }, []);

  const toggleSort = (key: keyof GameResult) => {
    if (key === sortKey) {
      setSortDir((prev) => (prev=== "asc" ? "desc" : "asc"));
    }else{
      setSortKey(key);
      setSortDir("asc");
    }
  };

  const fetchResults = async () => {
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/results');
      const data = await response.json();
      setResults(data);
    } catch (error) {
      console.error('Error fetching results:', error);
    } finally {
      setLoading(false);
    }
  };

  const withinDateRange = (d: Date) => {
    const fromOk = fromDate ? d >= new Date(fromDate + "T00:00:00") : true;
    const toOk   = toDate   ? d <= new Date(toDate   + "T23:59:59") : true;
    return fromOk && toOk;
  };

  const filtered = useMemo(() => {
    const s = search.trim().toLowerCase();
    return results.filter(r => {
      const d = new Date(r.gameDate);
      const matchesSearch = !s || r.playerName.toLowerCase().includes(s);
      const matchesWinner = !winnerFilter || r.winner === winnerFilter;
      const matchesDate   = withinDateRange(d);
      return matchesSearch && matchesWinner && matchesDate;
    });
  }, [results, search, winnerFilter, fromDate, toDate]);

  const stats = useMemo(() => {
    if (!filtered.length) {
      return {
        games: 0,
        playerWins: 0,
        computerWins: 0,
        ties: 0,
        winRate: 0,
        avgRounds: 0,
        avgPlayerScore: 0,
        avgComputerScore: 0,
      };
    }

    const games = filtered.length;
    let playerWins = 0, computerWins = 0, ties = 0;
    let sumRounds = 0, sumPS = 0, sumCS = 0;

    for (const r of filtered) {
      if (r.winner === "PLAYER") playerWins++;
      else if (r.winner === "COMPUTER") computerWins++;
      else ties++;

      sumRounds += r.rounds;
      sumPS += r.playerScore;
      sumCS += r.computerScore;
    }

    return {
      games,
      playerWins,
      computerWins,
      ties,
      winRate: playerWins / games,
      avgRounds: sumRounds / games,
      avgPlayerScore: sumPS / games,
      avgComputerScore: sumCS / games,
    };
  }, [filtered]);



  const sorted = useMemo(() => {
    const copy = [...filtered];
    copy.sort((a, b) => {
      if (sortKey === "gameDate") {
        const ta = new Date(a.gameDate).getTime();
        const tb = new Date(b.gameDate).getTime();
        return sortDir === "asc" ? ta - tb : tb - ta;
      }
      const va = a[sortKey] as any;
      const vb = b[sortKey] as any;
      if (typeof va === "number" && typeof vb === "number") {
        return sortDir === "asc" ? va - vb : vb - va;
      }
      const sa = String(va);
      const sb = String(vb);
      return sortDir === "asc" ? sa.localeCompare(sb) : sb.localeCompare(sa);
    });
    return copy;
  }, [filtered, sortKey, sortDir]);


  return (
      <div className="results-page">
        <h1>Game Results</h1>

        <div className="results-sections">
          {/* Actions */}
          <div className="section actions">
            <button onClick={fetchResults} className="refresh-btn">🔄 Refresh</button>
          </div>

          {/* Overview */}
          <section className="card section">
            <h2 className="section-title">Overview</h2>
            <div className="results-summary">
              <div className="summary-row">
                <div className="chip"><span>Total games</span><strong>{stats.games}</strong></div>
                <div className="chip win"><span>Player wins</span><strong>{stats.playerWins}</strong></div>
                <div className="chip lose"><span>Computer wins</span><strong>{stats.computerWins}</strong></div>
                <div className="chip tie"><span>Ties</span><strong>{stats.ties}</strong></div>
              </div>
              <div className="summary-row">
                <div className="chip"><span>Win rate</span><strong>{(stats.winRate * 100).toFixed(1)}%</strong></div>
                <div className="chip"><span>Avg rounds</span><strong>{stats.avgRounds.toFixed(1)}</strong></div>
                <div className="chip">
                  <span>Avg scores (You / CPU)</span>
                  <strong>{stats.avgPlayerScore.toFixed(1)} / {stats.avgComputerScore.toFixed(1)}</strong>
                </div>
              </div>
            </div>
          </section>

          {/* Filters */}
          <section className="card section">
            <h2 className="section-title">Filters</h2>
            <div className="filters-grid">
              <input
                  type="text"
                  placeholder="Search by player..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  aria-label="Search by player"
              />
              <select
                  value={winnerFilter}
                  onChange={(e) => setWinnerFilter(e.target.value as any)}
                  aria-label="Filter by winner"
              >
                <option value="">All winners</option>
                <option value="PLAYER">Player wins</option>
                <option value="COMPUTER">Computer wins</option>
                <option value="TIE">Ties</option>
              </select>
              <input
                  type="date"
                  value={fromDate}
                  onChange={(e) => setFromDate(e.target.value)}
                  aria-label="From date"
              />
              <input
                  type="date"
                  value={toDate}
                  onChange={(e) => setToDate(e.target.value)}
                  aria-label="To date"
              />
            </div>
          </section>

          <section className="card section">

            {loading ? (
                <p>Loading results...</p>
            ) : results.length === 0 ? (
                <p>No game results yet. Play some games!</p>
            ) : filtered.length === 0 ? (
                <p>No results match your filters.</p>
            ) : (
                <table className="results-table">
                  <thead>
                  <tr>
                    <th
                        onClick={() => toggleSort("playerName")}
                        aria-sort={sortKey === "playerName" ? (sortDir === "asc" ? "ascending" : "descending") : "none"}
                    >
                      Player {sortKey === "playerName" ? (sortDir === "asc" ? "▲" : "▼") : ""}
                    </th>
                    <th
                        onClick={() => toggleSort("playerScore")}
                        aria-sort={sortKey === "playerScore" ? (sortDir === "asc" ? "ascending" : "descending") : "none"}
                    >
                      Player Score {sortKey === "playerScore" ? (sortDir === "asc" ? "▲" : "▼") : ""}
                    </th>
                    <th
                        onClick={() => toggleSort("computerScore")}
                        aria-sort={sortKey === "computerScore" ? (sortDir === "asc" ? "ascending" : "descending") : "none"}
                    >
                      Computer Score {sortKey === "computerScore" ? (sortDir === "asc" ? "▲" : "▼") : ""}
                    </th>
                    <th
                        onClick={() => toggleSort("winner")}
                        aria-sort={sortKey === "winner" ? (sortDir === "asc" ? "ascending" : "descending") : "none"}
                    >
                      Winner {sortKey === "winner" ? (sortDir === "asc" ? "▲" : "▼") : ""}
                    </th>
                    <th
                        onClick={() => toggleSort("rounds")}
                        aria-sort={sortKey === "rounds" ? (sortDir === "asc" ? "ascending" : "descending") : "none"}
                    >
                      Rounds {sortKey === "rounds" ? (sortDir === "asc" ? "▲" : "▼") : ""}
                    </th>
                    <th
                        onClick={() => toggleSort("gameDate")}
                        aria-sort={sortKey === "gameDate" ? (sortDir === "asc" ? "ascending" : "descending") : "none"}
                    >
                      Date {sortKey === "gameDate" ? (sortDir === "asc" ? "▲" : "▼") : ""}
                    </th>
                  </tr>
                  </thead>
                  <tbody>
                  {sorted.map((result) => (
                      <tr key={result.id} className={result.winner === "PLAYER" ? "win" : ""}>
                        <td>{result.playerName}</td>
                        <td>{result.playerScore}</td>
                        <td>{result.computerScore}</td>
                        <td>
                          {result.winner === "PLAYER" ? "🏆 Player" :
                              result.winner === "COMPUTER" ? "🤖 Computer" : "🤝 Tie"}
                        </td>
                        <td>{result.rounds}</td>
                        <td>{new Date(result.gameDate).toLocaleString()}</td>
                      </tr>
                  ))}
                  </tbody>
                </table>
            )}
          </section>
        </div>
      </div>

  );
};

// ==================== PROFILE PAGE ================
const ProfilePage: React.FC<{ username: string; onLogout: () => void }> = ({ username, onLogout }) => {
  const [results, setResults] = useState<GameResult[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>("");

  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);

  const confirmLogout = async () => {
    try {
      await fetch("http://localhost:8080/api/logout", { method: "POST" }).catch(() => {});
    } finally {
      onLogout();
    }
  };

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError("");
        const res = await fetch('http://localhost:8080/api/results');
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data: GameResult[] = await res.json();
        setResults(data);
      } catch (e) {
        console.error(e);
        setError("Couldn't load your stats right now.");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const mine = useMemo(() => results.filter(r => r.playerName === username), [results, username]);

  const myStats = useMemo(() => {
    if (!mine.length) {
      return {
        games: 0, playerWins: 0, computerWins: 0, ties: 0,
        winRate: 0, avgRounds: 0, avgPlayerScore: 0, avgComputerScore: 0
      };
    }
    const games = mine.length;
    let playerWins = 0, computerWins = 0, ties = 0;
    let sumRounds = 0, sumPS = 0, sumCS = 0;
    for (const r of mine) {
      if (r.winner === "PLAYER") playerWins++;
      else if (r.winner === "COMPUTER") computerWins++;
      else ties++;
      sumRounds += r.rounds;
      sumPS += r.playerScore;
      sumCS += r.computerScore;
    }

    return {
      games,
      playerWins,
      computerWins,
      ties,
      winRate: games ? playerWins / games : 0,
      avgRounds: games ? sumRounds / games : 0,
      avgPlayerScore: games ? sumPS / games : 0,
      avgComputerScore: games ? sumCS / games : 0
    };
  }, [mine]);

  const badgeResults = useMemo(() => {
    const ctx = { stats: myStats, results: mine };
    return BADGES.map(b => {
      const { earned, progress } = b.condition(ctx);
      return { badge: b, earned, progress: progress ?? 0 };
    });
  }, [myStats, mine]);

  const accolades = useMemo(() => {
    const a: string[] = [];
    if (myStats.games >= 1) a.push("First Game");
    if (myStats.playerWins >= 1) a.push("First Win");
    if (myStats.games >= 10) a.push("Played 10 Games");
    if (myStats.games >= 25) a.push("Veteran: 25 Games");
    if (myStats.winRate >= 0.5 && myStats.games >= 5) a.push("Winning Record (50%+)");
    if (myStats.winRate === 1 && myStats.games >= 5) a.push("Flawless: 5 Wins, 0 Losses");
    return a;
  }, [myStats]);

  const handleLogoutClick = async () => {
    const confirmed = window.confirm("Are you sure you want to log out?");
    if (!confirmed) return;

    try {
      await fetch("http://localhost:8080/api/logout", { method: "POST" }).catch(() => {});
    } finally {
      onLogout();
    }
  };

  return (
      <div className="profile-page">
        <h1>Profile</h1>

        <section className="card section">
          <div className="profile-card">
            <div className="profile-left">
              <div className="avatar">{username?.[0]?.toUpperCase() || "U"}</div>
              <div>
                <p className="label">Username</p>
                <p className="value">{username}</p>
              </div>
            </div>

            <button onClick={() => setShowLogoutConfirm(true)} className="logout-btn logout-btn--profile">
              Log Out
            </button>
          </div>
        </section>

        <section className="card section">
          <h2 className="section-title">Your Stats</h2>
          {loading ? (
              <p>Loading…</p>
          ) : error ? (
              <p className="error">{error}</p>
          ) : mine.length === 0 ? (
              <p>No games saved under your username yet. Play a match!</p>
          ) : (
              <div className="results-summary">
                <div className="summary-row">
                  <div className="chip"><span>Total games</span><strong>{myStats.games}</strong></div>
                  <div className="chip win"><span>Player wins</span><strong>{myStats.playerWins}</strong></div>
                  <div className="chip lose"><span>Computer wins</span><strong>{myStats.computerWins}</strong></div>
                  <div className="chip tie"><span>Ties</span><strong>{myStats.ties}</strong></div>
                </div>
                <div className="summary-row">
                  <div className="chip"><span>Win rate</span><strong>{(myStats.winRate * 100).toFixed(1)}%</strong></div>
                  <div className="chip"><span>Avg rounds</span><strong>{myStats.avgRounds.toFixed(1)}</strong></div>
                  <div className="chip">
                    <span>Avg scores (You / CPU)</span>
                    <strong>{myStats.avgPlayerScore.toFixed(1)} / {myStats.avgComputerScore.toFixed(1)}</strong>
                  </div>
                </div>
              </div>
          )}
        </section>

        <section className="card section">
          <h2 className="section-title">Accolades</h2>
          <div className="badges-grid">
            {badgeResults.map(({badge, earned, progress}) => (
                <div key={badge.id} className={`badge-card ${earned ? "earned" : "locked"}`}>
                  <div className="badge-icon">{badge.icon}</div>
                  <div className="badge-info">
                    <div className="badge-name">{badge.name}</div>
                    <div className="badge-desc">{badge.description}</div>
                    {badge.maxProgress && (
                        <div className="badge-progress">
                          <div
                              className="badge-progress-fill"
                              style={{width: `${(progress / badge.maxProgress) * 100}%`}}
                          />
                          <span className="badge-progress-text">
                {progress}/{badge.maxProgress}
              </span>
                        </div>
                    )}
                  </div>
                </div>
            ))}
          </div>
        </section>

        {showLogoutConfirm && (
            <div className="modal" role="dialog" aria-modal="true" aria-labelledby="logout-title">
              <div className="modal-content confirm-modal">
                <h3 id="logout-title">Log out?</h3>
                <p className="confirm-text">You’ll be returned to the login screen.</p>

                <div className="modal-buttons">
                  <button onClick={confirmLogout} className="danger">Yes, log out</button>
                  <button onClick={() => setShowLogoutConfirm(false)} className="secondary">Cancel</button>
                </div>
              </div>
            </div>
        )}

      </div>
  );
};


// ==================== MAIN APP ====================
function App() {
  const [currentPage, setCurrentPage] = useState<'auth' | 'game' | 'results' | 'profile'>('auth');
  const [username, setUsername] = useState<string>('');
  const [userId, setUserId] = useState<number | null>(null);

  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ type, message });
    // auto-hide after 3s
    setTimeout(() => setToast(null), 3000);
  };

  const handleLogin = (user: string, id: number) => {
    setUsername(user);
    setUserId(id);
    setCurrentPage('game');
  };

  const handleLogout = () => {
    setUsername('');
    setUserId(null);
    setCurrentPage('auth');
  };

  return (
      <div className="app">
        {currentPage !== 'auth' && (
            <nav className="navbar">
              <div className="nav-brand">Arabian Card Game</div>
              <div className="nav-links">
              <button
                    onClick={() => setCurrentPage('game')}
                    className={currentPage === 'game' ? 'active' : ''}
                >
                  🎮 Play Game
                </button>
                <button
                    onClick={() => setCurrentPage('results')}
                    className={currentPage === 'results' ? 'active' : ''}
                >
                  📊 Results
                </button>
                <button
                    onClick={() => setCurrentPage('profile')}
                    className={currentPage === 'profile' ? 'active' : ''}
                >
                  👤 Profile
                </button>
              </div>
            </nav>
        )}

        {toast && (
            <div className={`toast ${toast.type === 'success' ? 'toast-success' : 'toast-error'}`} role="status" aria-live="polite">
              {toast.message}
            </div>
        )}

        <main className="main-content">
          {currentPage === 'auth' && <AuthPage onLogin={handleLogin} onNotify={showToast} />}
          {currentPage === 'game' && <GamePage username={username}/>}
          {currentPage === 'results' && <ResultsPage/>}
          {currentPage === 'profile' && <ProfilePage username={username} onLogout={handleLogout}/>}
        </main>
      </div>
  );
}

export default App;