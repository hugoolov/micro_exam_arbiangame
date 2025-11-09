# Arabian Card Game - Frontend

React + TypeScript + Vite frontend for the Arabian Card Game microservices project.

## Features

- **User Authentication**: Login and registration
- **Card Game**: Play against AI opponent
- **Game Results**: View leaderboard of past games
- **Weather Integration**: Real-time weather with "go outside" recommendations
- **Responsive Design**: Works on desktop and mobile

## Prerequisites

- Node.js 18+ 
- npm or yarn

## Setup Instructions

### 1. Install Dependencies

```bash
npm install
```

### 2. Start Development Server

```bash
npm run dev
```

The frontend will start at http://localhost:5173

### 3. Build for Production

```bash
npm run build
```

The built files will be in the `dist/` directory.

## Project Structure

```
frontend/
├── src/
│   ├── App.tsx          # Main application component
│   ├── App.css          # Styles
│   └── main.tsx         # Entry point
├── public/
│   └── card_svg/        # Card images (SVG files)
├── index.html           # HTML template
├── package.json         # Dependencies
├── vite.config.ts       # Vite configuration
└── tsconfig.json        # TypeScript configuration
```

## Card Images

Place your card SVG files in the `public/card_svg/` directory.

Expected file naming convention:
- `ace_of_hearts.svg`
- `2_of_spades.svg`
- `jack_of_diamonds.svg`
- etc.

## API Endpoints Used

The frontend communicates with these backend endpoints through the API Gateway (http://localhost:8080):

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login
- `GET /api/auth/check-username` - Check username availability
- `POST /api/game/start` - Start new game
- `POST /api/game/{id}/draw` - Draw a card
- `POST /api/game/{id}/complete-turn` - Complete player turn
- `POST /api/game/{id}/end` - End game early
- `POST /api/game/{id}/save-result` - Save game result
- `GET /api/results` - Get all game results
- `GET /api/weather` - Get current weather

## Environment Configuration

The frontend is configured to connect to:
- **API Gateway**: http://localhost:8080
- **Frontend Dev Server**: http://localhost:5173

To change these, edit `vite.config.ts` and update API URLs in `App.tsx`.

## Features Overview

### Navigation
- **Play Game**: Main game interface
- **Results**: View game history
- **Logout**: Return to login screen

### Game Flow
1. Login or register
2. View weather widget
3. Click "Start Game"
4. Draw cards from main deck or open table
5. Swap or discard drawn cards
6. Play until deck is empty
7. View final scores
8. Save result to leaderboard

### Weather Widget
- Shows current temperature, wind speed, and conditions
- Animates when weather is nice (sunny, 15-25°C)
- Encourages going outside in good weather
- Encourages gaming in bad weather

## Development

### Hot Module Replacement
Vite provides fast HMR - changes appear instantly during development.

### Type Checking
```bash
npm run build  # Runs TypeScript compiler
```

### Linting
The project uses TypeScript's strict mode for type safety.

## Technologies

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **CSS3** - Styling with animations

## Browser Support

- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)

## Troubleshooting

### CORS Errors
If you see CORS errors, ensure:
1. Backend services are running
2. API Gateway is configured correctly
3. No duplicate CORS headers

### Card Images Not Loading
Ensure card SVG files are in `public/card_svg/` directory with correct naming.

### Connection Refused
Verify the API Gateway is running on port 8080:
```bash
curl http://localhost:8080/actuator/health
```

## License

This project is for educational purposes as part of PG3402 Microservices course.
