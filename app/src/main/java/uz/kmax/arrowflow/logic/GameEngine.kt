package uz.kmax.arrowflow.logic

import uz.kmax.arrowflow.model.Arrow
import uz.kmax.arrowflow.model.Board
import uz.kmax.arrowflow.model.Direction
import uz.kmax.arrowflow.model.Level
import uz.kmax.arrowflow.model.LevelProgress

enum class GameState {
    PLAYING,
    LEVEL_COMPLETE,
    GAME_OVER
}

class GameEngine(private var board: Board) {

    private val listeners = mutableListOf<GameEngineListener>()
    private val progress = mutableMapOf<Int, LevelProgress>()

    var currentLevel: Level? = null
        private set

    var currentLives: Int = 3
        private set

    private val undoStack = ArrayDeque<Arrow>()
    var hintsUsed: Int = 0
        private set
    var undosUsed: Int = 0
        private set

    var mistakes: Int = 0
        private set

    var state: GameState = GameState.PLAYING
        private set

    private var lastLifeLostTime: Long = 0
    private val LIFE_LOSS_COOLDOWN = 250L

    interface GameEngineListener {
        fun onArrowExit(arrow: Arrow)
        fun onArrowBlocked(arrow: Arrow)
        fun onLivesChanged(lives: Int)
        fun onGameStateChanged(state: GameState, stars: Int = 0)
        fun onLevelLoaded(level: Level, board: Board)
    }

    fun addListener(listener: GameEngineListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: GameEngineListener) {
        listeners.remove(listener)
    }

    fun loadLevel(level: Level) {
        this.currentLevel = level
        this.board = Board(level)
        this.currentLives = 3
        this.mistakes = 0
        this.hintsUsed = 0
        this.undosUsed = 0
        undoStack.clear()
        this.state = GameState.PLAYING
        listeners.forEach { it.onLevelLoaded(level, board) }
        listeners.forEach { it.onLivesChanged(currentLives) }
        listeners.forEach { it.onGameStateChanged(state) }
    }

    fun handleCellTap(row: Int, col: Int) {
        if (state != GameState.PLAYING) return

        val arrow = board.getArrowAt(row, col) ?: return
        
        if (isPathClear(arrow)) {
            listeners.forEach { it.onArrowExit(arrow) }
            undoStack.addLast(arrow.copy())
            board.removeArrow(arrow)
        } else {
            loseLife()
            listeners.forEach { it.onArrowBlocked(arrow) }
        }
    }

    fun canUndo(): Boolean = state == GameState.PLAYING && undoStack.isNotEmpty()

    fun undoLast(): Boolean {
        if (!canUndo()) return false
        val arrow = undoStack.removeLast()
        board.restoreArrow(arrow)
        undosUsed++
        currentLevel?.let { listeners.forEach { l -> l.onLevelLoaded(it, board) } }
        return true
    }

    /** Hozir olib tashlasa bo'ladigan o'qni topadi (hint). */
    fun findHint(): Arrow? {
        if (state != GameState.PLAYING) return null
        hintsUsed++
        return board.getArrows().firstOrNull { isPathClear(it) }
    }

    fun isHintAvailable(arrow: Arrow): Boolean = state == GameState.PLAYING && isPathClear(arrow)

    private fun loseLife() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLifeLostTime < LIFE_LOSS_COOLDOWN) return
        lastLifeLostTime = currentTime

        mistakes++
        if (currentLives > 0) {
            currentLives--
            listeners.forEach { it.onLivesChanged(currentLives) }
            if (currentLives == 0) {
                state = GameState.GAME_OVER
                listeners.forEach { it.onGameStateChanged(state) }
            }
        }
    }

    fun onAnimationFinished() {
        if (board.isEmpty() && state == GameState.PLAYING) {
            state = GameState.LEVEL_COMPLETE
            val stars = calculateStars()
            updateProgress(stars)
            listeners.forEach { it.onGameStateChanged(state, stars) }
        }
    }

    private fun calculateStars(): Int {
        return when {
            mistakes == 0 -> 3
            mistakes <= 2 -> 2
            else -> 1
        }
    }

    private fun updateProgress(stars: Int) {
        val levelId = currentLevel?.id ?: return
        val currentBest = progress[levelId]?.bestStars ?: 0
        if (stars > currentBest) {
            progress[levelId] = LevelProgress(levelId, stars)
        }
    }

    fun getBestStars(levelId: Int): Int {
        return progress[levelId]?.bestStars ?: 0
    }

    fun getRemainingArrowsCount(): Int {
        return board.getArrows().size
    }

    private fun isPathClear(arrow: Arrow): Boolean {
        val dr = when (arrow.direction) {
            Direction.UP -> -1
            Direction.DOWN -> 1
            else -> 0
        }
        val dc = when (arrow.direction) {
            Direction.LEFT -> -1
            Direction.RIGHT -> 1
            else -> 0
        }

        // SLITHERING LOGIC:
        // An arrow slithers forward out through its HEAD.
        // The path in front of the HEAD in arrow.direction to the edge of the board must be clear.
        val head = arrow.points.lastOrNull() ?: return true
        var currR = head.first + dr
        var currC = head.second + dc

        while (currR in 0 until board.size && currC in 0 until board.size) {
            val other = board.getArrowAt(currR, currC)
            if (other != null && other.id != arrow.id) {
                return false
            }
            currR += dr
            currC += dc
        }

        return true
    }
}