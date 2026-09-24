# Project Modularization and Veteran Level Design

This plan outlines the restructuring of the level management system and the implementation of a high-quality level generation algorithm based on "Veteran Game Developer" principles.

## User Review Required

> [!IMPORTANT]
> **Level Data Reset**: The existing `levels.json` has been split into chunks (1-100, 101-200, etc.). I recommend regenerating these files using the new "Veteran" logic to ensure the requested density and quality.
> **Solvability Check**: While the new generator creates high-density puzzles, very high levels (e.g., 500+) might require a few seconds of generation time on-device if not pre-cached in JSON.

## Proposed Changes

### Level Data Layer

#### [MODIFY] [LevelRepository.kt](file:///C:/Users/User/AndroidStudioProjects/ArrowFlow/app/src/main/java/uz/kmax/arrowflow/logic/LevelRepository.kt)
- Changed from monolithic loading to **on-demand chunk loading**.
- Supports arbitrary JSON additions to `assets/levels/`.
- Implements internal caching for performance.

#### [NEW] `assets/levels/` Directory
- Contains `levels_1_100.json`, `levels_101_200.json`, etc.
- Allows for easy modular updates without touching the main code.

### Level Generation Logic

#### [MODIFY] [LevelGenerator.kt](file:///C:/Users/User/AndroidStudioProjects/ArrowFlow/app/src/main/java/uz/kmax/arrowflow/logic/LevelGenerator.kt)
- **Flow-based Generation**: Implemented a "Momentum" system to avoid jarring left-right zigzags.
- **Gap Filling**: A secondary phase that scans the grid for empty cells and packs them with smaller arrows to reach maximum density.
- **Complexity Scaling**: More logical progression of grid sizes (5x5 -> 15x15) and winding snake lengths.

## Verification Plan

### Automated Verification
- Run a script to verify that all 7 chunks are valid JSON and contain 100 levels each.
- Test `LevelRepository.getLevel(id)` for IDs 1, 101, 201 to ensure chunk switching works.

### Manual Verification
- Visual inspection of generated levels using Compose Preview.
- Check for "jerky" turns in Level 50+ to ensure momentum logic is working.
