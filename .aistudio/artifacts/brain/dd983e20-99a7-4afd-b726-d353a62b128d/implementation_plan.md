# FunVerse: Color Match — Unlimited Levels & Progressive Challenges

Expand the casual match-3 game with an infinite procedural level generator, escalating multi-tier difficulty milestones, ice obstacles, color collection objectives, a Level Journey map, and occasional child-safe, non-intrusive interstitial ads.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product specifications were confirmed during interactive planning:

- **Secondary Mechanics & Obstacles**: At Level 51+, levels introduce **Color Collection Goals** (e.g. "Collect 15 Ruby & 15 Emerald tiles") and **Ice Tiles** (frozen tiles that break and unfreeze when adjacent matches occur).
- **Ad Frequency & Pacing**: Interstitial ads appear only after every **3–4 completed levels**, with **Levels 1–3 strictly 100% ad-free**. Ads never appear during gameplay or upon failed attempts, and offline play continues with zero interruption.
- **Level Navigation**: Added a **Level Journey screen** with a scrolling node pathway, star ratings (1–3 stars per cleared level), and a prominent "Continue Level" button, while retaining the Classic Difficulty Arcade mode.
- **100% Free Guarantee**: Zero paywalls, zero in-app purchases, zero forced ads for progression. All items and rewards are unlocked solely through puzzle-solving skill.

---

## 1. Overview & Core Concept

- **What It Does**: Upgrades FunVerse into an endless puzzle journey where players tackle unlimited levels (Level 1 to 100 and beyond). Every level is generated with distinct grid dimensions, color counts, move allowances, and clearable objectives.
- **Target Audience**: Casual puzzle lovers of all ages wanting an endless, relaxing yet brain-teasing match-3 experience on phones and tablets.
- **Key Value**: Endless replayability with a smooth, fair difficulty curve that avoids sudden difficulty spikes, preserves offline progress, and respects player time.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Level Journey & Select**:
   - From the Main Menu, players can tap **"Journey Mode"** (with current Level badge and total stars) or **"Arcade Mode"** (Quick Easy/Medium/Hard).
   - In Journey Mode, a cosmic starfield map displays completed levels with stars, current active level, and upcoming locked levels.
2. **Gameplay with Dynamic Objectives**:
   - **Levels 1–10 (Beginner)**: Relaxed 4×4 to 5×5 grids, 3–4 colors, generous move limits (25–30 moves), target score goal.
   - **Levels 11–25 (Novice)**: 5×5 to 6×6 grids, 4–5 colors, 22–26 moves, introducing specific color targets.
   - **Levels 26–50 (Challenger)**: 6×6 to 7×7 grids, 5 colors, 20–24 moves, dual target scores and color quota goals.
   - **Levels 51–100 (Master)**: 7×7 to 8×8 grids, 5–6 colors, **Ice Tiles** layered over grid slots that crack upon neighboring matches, plus collection goals.
   - **Levels 101+ (Infinite Legend)**: Procedural scaling algorithm continuously mixes ice layers, collection quotas, and tight move margins while guaranteeing solvable start boards.
3. **Level Cleared & Ad Flow**:
   - On clearing a level, the triumphant **Level Complete Dialog** shows stars earned and score.
   - After tapping "Next Level", if the level index is > 3 and satisfies the 3–4 level pacing interval, a short, child-safe interstitial ad screen appears with a 5-second countdown and clear skip/close button. Offline players bypass this instantly without waiting.
4. **Game Over & Fair Retry**:
   - If moves reach zero before completing the goal, the Game Over dialog offers a free instant retry without penalizing the player or forcing an ad.

### Visual Identity & Theme
- **Background**: Deep galaxy midnight purple (`#0D0B1E` to `#161233`) with glowing nebula cards.
- **Ice Tiles**: Translucent crystalline cyan overlays (`#80E1FD`) with frosty frosted-glass borders and fracture animations when cracked.
- **Objective HUD**: Clean pill badges in the game header showing real-time remaining target quotas (e.g. `Ruby: 8/15`, `Ice: 4/8`).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Deterministic Procedural Level Generator**
  - *Approach*: Use a mathematical level configuration generator `LevelConfigGenerator.generate(level: Int)` that derives grid size (4 to 8), color count (3 to 6), move limit (18 to 32), ice tile count, and color quotas based on level milestones.
  - *Why*: Eliminates fixed level caps, guarantees zero repetitive boards, and ensures mathematical solvability without bloating APK storage.
- **Decision 2: Local Persistence of Highest Reached Level & Stars**
  - *Approach*: Store unlocked level index, stars per level (1–3), and total accumulated stars in `SharedPreferences`.
  - *Why*: Zero network dependency, completely private, survives app restarts.
- **Decision 3: Non-Intrusive Interstitial Ad Integration with Offline Grace**
  - *Approach*: An `AdManager` architecture that manages Google AdMob test interstitial IDs (`ca-app-pub-3940256099942544/1033173712`) paired with a simulated fallback player.
  - *Why*: Satisfies Google Play family and ad policies (strictly post-level, max once every 3-4 levels, never mid-game, 100% playable offline).

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                       MAIN ACTIVITY                         │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
        ┌──────▼──────────────┐        ┌───────▼──────────────┐
        │   MAIN MENU SCREEN  │        │ LEVEL JOURNEY SCREEN │
        │ (Arcade vs Journey) │        │ (Map, Stars, Select) │
        └──────┬──────────────┘        └───────┬──────────────┘
               │                               │
               └───────────────┬───────────────┘
                               │
                      ┌────────▼────────┐
                      │ GAMEPLAY SCREEN │
                      └────────┬────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
   ┌──────▼──────┐      ┌──────▼──────┐      ┌──────▼──────┐
   │ OBJECTIVE   │      │ GAME BOARD  │      │ AD / DIALOG │
   │ HUD HEADER  │      │ (Tiles/Ice) │      │ CONTROLLER  │
   └─────────────┘      └──────┬──────┘      └─────────────┘
                               │
┌──────────────────────────────┴──────────────────────────────┐
│                    GAME ENGINE & VIEWMODEL                  │
│  - Procedural Level Generator (Levels 1 to 100+)            │
│  - Match-3 + Cascade Engine with Ice Cracking               │
│  - Color Quota Collection Tracker                           │
│  - PreferencesManager (Unlocked Level, Stars, High Scores)  │
│  - AdManager (Pacing Counter, First 3 Levels Free)          │
└─────────────────────────────────────────────────────────────┘
```

### Key Components to Add/Update:
1. `model/LevelModels.kt`:
   - `data class LevelObjective(val targetScore: Int, val requiredColors: Map<TileColor, Int>, val totalIceTiles: Int)`
   - `data class LevelConfig(val levelNumber: Int, val gridSize: Int, val colorCount: Int, val moves: Int, val objective: LevelObjective)`
2. `game/LevelGenerator.kt`:
   - Procedural generator computing progressive difficulty curves for any integer level $N \ge 1$.
3. `data/PreferencesManager.kt`:
   - Save/retrieve highest unlocked level and star ratings per level.
4. `ads/AdManager.kt`:
   - Enforce 3–4 level pacing interval, first 3 levels ad-free rule, and child-safe timeout handling.
5. `ui/screens/LevelJourneyScreen.kt`:
   - Cosmic roadmap screen showing cleared levels, earned stars, and "Continue Journey".
6. `ui/components/TileView.kt` & `GameBoardView.kt`:
   - Render frosty ice tile overlays and cracking animations.
