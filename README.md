# ChessFlow ♟️
A modern Android chess puzzle trainer built with Jetpack Compose and powered by the Stockfish engine.

## 🔥 Features
- Solve chess puzzles by difficulty
- Visual board with animated moves
- Hints and Stockfish evaluation
- Offline mode
- Firebase integration
- Persistent progress with DataStore

### How to use
1. Select your preferred puzzle difficulty.
2. Find the best move for the current position.
3. Use hints if you get stuck—powered by the world's strongest engine!

## ⚙️ Tech Stack
- Kotlin + Jetpack Compose
- Hilt (Dependency Injection)
- Firebase Realtime Database
- DataStore for preferences
- Stockfish chess engine (GPLv3)

🛠️ Setup Note
Due to GitHub's file size limits, the Stockfish NNUE model file is not included in this repository. Please download nn-c288c895ea92.nnue and nn-37f18f62d772.nnue and place it in the app/src/main/assets/ folder before building.

## ⚖️ License and Credits
This app uses the open-source [Stockfish](https://stockfishchess.org) engine, licensed under the **GNU General Public License v3 (GPLv3)**.

You may use, modify, and distribute this app under the same license.

Full license text: [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.html)

## 👩‍💻 Developer
Developed with ❤️ by Yanka Mincheva.

---

© 2026 ChessFlow. All rights reserved.
