package uz.kmax.arrowflow.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.kmax.arrowflow.logic.GameEngine
import uz.kmax.arrowflow.logic.GameState
import uz.kmax.arrowflow.logic.LevelRepository
import uz.kmax.arrowflow.logic.ProgressRepository
import uz.kmax.arrowflow.model.Arrow

data class GameUiState(
    val levelId: Int = 1,
    val arrowsLeft: Int = 0,
    val lives: Int = 3,
    val canUndo: Boolean = false,
    val hintsLeft: Int = 5,
    val hintArrowId: Int? = null,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val state: GameState = GameState.PLAYING,
    val stars: Int = 0
)

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Use nullable instead of lateinit to avoid UninitializedPropertyAccessException
    private var engine: GameEngine? = null
    private var repo: ProgressRepository? = null
    private var combo = 0
    private var bestCombo = 0

    fun init(levelId: Int, repository: ProgressRepository, gameEngine: GameEngine) {
        repo = repository
        engine = gameEngine
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                levelId = levelId,
                hintsLeft = repository.hintsLeft.first()
            )
        }
    }

    fun onArrowsChanged() {
        val e = engine ?: return
        _uiState.value = _uiState.value.copy(
            arrowsLeft = e.getRemainingArrowsCount(),
            canUndo = e.canUndo()
        )
    }

    fun onMoveSuccess() {
        combo++
        if (combo > bestCombo) bestCombo = combo
        _uiState.value = _uiState.value.copy(combo = combo, bestCombo = bestCombo)
        onArrowsChanged()
    }

    fun onMoveBlocked() {
        combo = 0
        _uiState.value = _uiState.value.copy(combo = 0)
    }

    fun onLives(lives: Int) {
        _uiState.value = _uiState.value.copy(lives = lives)
    }

    fun onState(state: GameState, stars: Int = 0) {
        _uiState.value = _uiState.value.copy(state = state, stars = stars)
        if (state == GameState.PLAYING) {
            combo = 0
            _uiState.value = _uiState.value.copy(combo = 0, hintArrowId = null)
            onArrowsChanged()
        }
    }

    fun undo(onDone: (Boolean) -> Unit) {
        val e = engine ?: run { onDone(false); return }
        val r = repo ?: run { onDone(false); return }
        val ok = e.undoLast()
        if (ok) {
            viewModelScope.launch { r.recordUndo() }
            _uiState.value = _uiState.value.copy(hintArrowId = null)
            onArrowsChanged()
        }
        onDone(ok)
    }

    fun useHint(onResult: (Arrow?) -> Unit) {
        val r = repo ?: run { onResult(null); return }
        val e = engine ?: run { onResult(null); return }
        viewModelScope.launch {
            val left = r.hintsLeft.first()
            if (left <= 0) {
                onResult(null)
                return@launch
            }
            val hint = e.findHint()
            if (hint == null) {
                onResult(null)
                return@launch
            }
            if (r.useHint()) {
                _uiState.value = _uiState.value.copy(
                    hintsLeft = r.hintsLeft.first(),
                    hintArrowId = hint.id
                )
                onResult(hint)
            } else onResult(null)
        }
    }

    fun clearHint() {
        _uiState.value = _uiState.value.copy(hintArrowId = null)
    }

    fun saveProgress(stars: Int, onSaved: () -> Unit = {}) {
        val e = engine ?: return
        val r = repo ?: return
        viewModelScope.launch {
            val level = e.currentLevel ?: return@launch
            r.saveProgress(level.id, stars)
            onSaved()
        }
    }

    fun nextLevelId(): Int? {
        val e = engine ?: return null
        val cur = e.currentLevel?.id ?: return null
        return LevelRepository.getNextLevel(cur)?.id
    }
}
