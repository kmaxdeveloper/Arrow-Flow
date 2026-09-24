package uz.kmax.arrowflow.logic

import uz.kmax.arrowflow.model.Arrow
import uz.kmax.arrowflow.model.Difficulty
import uz.kmax.arrowflow.model.Direction
import uz.kmax.arrowflow.model.Level
import kotlin.random.Random

/**
 * Solvable & Beautiful Level Generator.
 * Creates long, winding snake arrows with clean flow and head-first slithering.
 * Guarantees 100% solvability and high grid utilization without visual bugs.
 */
object LevelGenerator {

    fun generate(id: Int): Level {
        val random = Random((id * 777L) + 42L)

        val size = when {
            id <= 30 -> 5
            id <= 70 -> 6
            id <= 100 -> 7
            id <= 200 -> 7
            id <= 300 -> 8
            id <= 600 -> 9
            else -> 10
        }

        val difficulty = when {
            id <= 100 -> Difficulty.EASY
            id <= 300 -> Difficulty.MEDIUM
            else -> Difficulty.HARD
        }

        val targetDensity = when (difficulty) {
            Difficulty.EASY -> 0.75f
            Difficulty.MEDIUM -> 0.82f
            Difficulty.HARD -> 0.88f
        }

        val allowedLengths = when (difficulty) {
            Difficulty.EASY -> listOf(3, 4)
            Difficulty.MEDIUM -> listOf(3, 4, 5)
            Difficulty.HARD -> listOf(4, 5, 6, 7)
        }

        val targetCount = (size * size * targetDensity).toInt()
        val occupiedBoard = mutableSetOf<Pair<Int, Int>>()
        val placedArrows = mutableListOf<Arrow>()

        var attempts = 0
        val maxAttempts = 3000

        while (occupiedBoard.size < targetCount && attempts < maxAttempts) {
            attempts++

            val length = allowedLengths[random.nextInt(allowedLengths.size)]
            val dir = Direction.entries[random.nextInt(4)]
            val dr = when (dir) {
                Direction.UP -> -1; Direction.DOWN -> 1; else -> 0
            }
            val dc = when (dir) {
                Direction.LEFT -> -1; Direction.RIGHT -> 1; else -> 0
            }

            val headR = random.nextInt(size)
            val headC = random.nextInt(size)

            if ((headR to headC) in occupiedBoard) continue
            if (!canHeadExit(headR, headC, dir, size, occupiedBoard)) continue

            val prevR = headR - dr
            val prevC = headC - dc

            if (prevR !in 0 until size || prevC !in 0 until size) continue
            if ((prevR to prevC) in occupiedBoard) continue

            val currentPath = mutableListOf(prevR to prevC, headR to headC)
            val visitedInArrow = mutableSetOf(prevR to prevC, headR to headC)

            var currBackR = prevR
            var currBackC = prevC
            var backDr = -dr
            var backDc = -dc
            var turnCooldown = 0

            repeat(length - 2) {
                val possibleBackDirs = mutableListOf(backDr to backDc)
                if (turnCooldown == 0) {
                    if (backDr != 0) {
                        possibleBackDirs.add(0 to 1)
                        possibleBackDirs.add(0 to -1)
                    } else {
                        possibleBackDirs.add(1 to 0)
                        possibleBackDirs.add(-1 to 0)
                    }
                }
                possibleBackDirs.shuffle(random)

                var stepAdded = false
                for ((bDr, bDc) in possibleBackDirs) {
                    val nextR = currBackR + bDr
                    val nextC = currBackC + bDc

                    if (nextR in 0 until size && nextC in 0 until size &&
                        (nextR to nextC) !in occupiedBoard &&
                        (nextR to nextC) !in visitedInArrow
                    ) {
                        currentPath.add(0, nextR to nextC)
                        visitedInArrow.add(nextR to nextC)

                        if (bDr != backDr || bDc != backDc) {
                            backDr = bDr
                            backDc = bDc
                            turnCooldown = 2
                        } else if (turnCooldown > 0) {
                            turnCooldown--
                        }

                        currBackR = nextR
                        currBackC = nextC
                        stepAdded = true
                        break
                    }
                }
                if (!stepAdded) return@repeat
            }

            if (currentPath.size >= 2 && canHeadExit(headR, headC, dir, size, occupiedBoard)) {
                placedArrows.add(Arrow(id = 0, points = currentPath, direction = dir))
                occupiedBoard.addAll(currentPath)
                attempts = 0
            }
        }

        // Reverse placedArrows so that lower IDs match first solvable moves in gameplay.
        val finalArrows = placedArrows.reversed().mapIndexed { index, arrow ->
            arrow.copy(id = index + 1)
        }

        return Level(id = id, size = size, difficulty = difficulty, arrows = finalArrows)
    }

    private fun canHeadExit(headR: Int, headC: Int, dir: Direction, size: Int, occupied: Set<Pair<Int, Int>>): Boolean {
        val dr = when (dir) {
            Direction.UP -> -1; Direction.DOWN -> 1; else -> 0
        }
        val dc = when (dir) {
            Direction.LEFT -> -1; Direction.RIGHT -> 1; else -> 0
        }
        var currR = headR + dr
        var currC = headC + dc
        while (currR in 0 until size && currC in 0 until size) {
            if ((currR to currC) in occupied) return false
            currR += dr
            currC += dc
        }
        return true
    }
}
