import { useState, useEffect } from 'react';
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
const AuthPage: React.FC<{ onLogin: (username: string, userId: number) => void }> = ({ onLogin }) => {
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

  const handleRegister = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: newUsername, password: newPassword }),
      });

      if (response.ok) {
        const data = await response.json();
        alert(`User ${data.username} registered successfully!`);
        setShowRegister(false);
        setNewUsername('');
        setNewPassword('');
      } else {
        alert(`Registration failed: ${await response.text()}`);
      }
    } catch {
      alert('Something went wrong during registration');
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

// ==================== GAME COMPONENT ====================
const GamePage: React.FC<{ username: string }> = ({ username }) => {
  const [gameState, setGameState] = useState<any>(null);
  const [drawnCard, setDrawnCard] = useState<any>(null);
  const [selectedCardIndex, setSelectedCardIndex] = useState<number | null>(null);
  const [drawFrom, setDrawFrom] = useState<string | null>(null);

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

  return (
    <div className="game-page">
      <h1>Card Game</h1>
      <p>Playing as: <strong>{username}</strong></p>

      {!gameState && (
        <button onClick={startGame} className="start-game-btn">Start New Game</button>
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
              <button onClick={endGame} className="secondary">End Game Early</button>
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
    </div>
  );
};

// ==================== RESULTS COMPONENT ====================
const ResultsPage: React.FC = () => {
  const [results, setResults] = useState<GameResult[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchResults();
  }, []);

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

  return (
    <div className="results-page">
      <h1>Game Results</h1>
      <button onClick={fetchResults} className="refresh-btn">🔄 Refresh</button>

      {loading ? (
        <p>Loading results...</p>
      ) : results.length === 0 ? (
        <p>No game results yet. Play some games!</p>
      ) : (
        <table className="results-table">
          <thead>
            <tr>
              <th>Player</th>
              <th>Player Score</th>
              <th>Computer Score</th>
              <th>Winner</th>
              <th>Rounds</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {results.map((result) => (
              <tr key={result.id} className={result.winner === 'PLAYER' ? 'win' : ''}>
                <td>{result.playerName}</td>
                <td>{result.playerScore}</td>
                <td>{result.computerScore}</td>
                <td>
                  {result.winner === 'PLAYER' ? '🏆 Player' : 
                   result.winner === 'COMPUTER' ? '🤖 Computer' : '🤝 Tie'}
                </td>
                <td>{result.rounds}</td>
                <td>{new Date(result.gameDate).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

// ==================== MAIN APP ====================
function App() {
  const [currentPage, setCurrentPage] = useState<'auth' | 'game' | 'results'>('auth');
  const [username, setUsername] = useState<string>('');
  const [userId, setUserId] = useState<number | null>(null);

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
            <button onClick={handleLogout} className="logout-btn">
              🚪 Logout
            </button>
          </div>
        </nav>
      )}

      <main className="main-content">
        {currentPage === 'auth' && <AuthPage onLogin={handleLogin} />}
        {currentPage === 'game' && <GamePage username={username} />}
        {currentPage === 'results' && <ResultsPage />}
      </main>
    </div>
  );
}

export default App;
