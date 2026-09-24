package uz.kmax.arrowflow.ui.game

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import uz.kmax.arrowflow.R
import uz.kmax.arrowflow.databinding.FragmentGameBinding
import uz.kmax.arrowflow.logic.AdsManager
import uz.kmax.arrowflow.logic.FeedbackManager
import uz.kmax.arrowflow.logic.GameEngine
import uz.kmax.arrowflow.logic.GameState
import uz.kmax.arrowflow.logic.LevelRepository
import uz.kmax.arrowflow.logic.ProgressRepository
import uz.kmax.arrowflow.model.Arrow
import uz.kmax.arrowflow.model.Board
import uz.kmax.arrowflow.model.Level
import uz.kmax.base.fragment.BaseFragment
import java.util.Locale

class GameFragment : BaseFragment(R.layout.fragment_game), GameEngine.GameEngineListener {

    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private lateinit var gameEngine: GameEngine
    private lateinit var progressRepository: ProgressRepository
    private lateinit var feedbackManager: FeedbackManager
    private val viewModel: GameViewModel by viewModels()

    private var previousLives: Int = 3

    override fun onViewCreated() {
        _binding = FragmentGameBinding.bind(layout)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, systemBars.bottom)
            insets
        }
        progressRepository = ProgressRepository(requireContext())
        feedbackManager = FeedbackManager(requireContext(), progressRepository)
        
        val levelId = arguments?.getInt("levelId") ?: 1
        val initialLevel = LevelRepository.getLevel(levelId) ?: LevelRepository.getLevel(1)!!
        
        gameEngine = GameEngine(Board(initialLevel))
        viewModel.init(levelId, progressRepository, gameEngine)
        
        gameEngine.addListener(this)
        gameEngine.addListener(binding.gameBoard)
        
        binding.gameBoard.setEngine(gameEngine)
        gameEngine.loadLevel(initialLevel)
        
        setupListeners()
        observeUiState()
        setupTutorial()
        resetOverlays()
        applyJuicyTouch(binding.btnHint, binding.btnTool, binding.btnBack, binding.btnSettings)
        
        AdsManager.loadInterstitial(requireContext())
        AdsManager.loadRewarded(requireContext())
        AdsManager.showBanner(binding.bannerContainer, requireActivity())
    }

    private fun applyJuicyTouch(vararg views: View) {
        views.forEach { view ->
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).setInterpolator(OvershootInterpolator()).start()
                        if (event.action == MotionEvent.ACTION_UP) v.performClick()
                    }
                }
                true
            }
        }
    }

    private fun setupListeners() {
        binding.btnHint.setOnClickListener {
            feedbackManager.onButtonClick(it)
            viewModel.useHint { hint ->
                if (hint == null) {
                    Toast.makeText(requireContext(), R.string.no_hints_left, Toast.LENGTH_SHORT).show()
                } else {
                    feedbackManager.onMoveSuccess(binding.gameBoard)
                }
            }
        }

        binding.btnTool.setOnClickListener {
            feedbackManager.onButtonClick(it)
            viewModel.undo { success ->
                if (success) {
                    feedbackManager.onMoveSuccess(binding.gameBoard)
                } else {
                    Toast.makeText(requireContext(), R.string.undo_unavailable, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnBack.setOnClickListener {
            feedbackManager.onButtonClick(it)
            AdsManager.showInterstitial(requireActivity()) {
                findNavController().navigateUp()
            }
        }

        binding.btnSettings.setOnClickListener {
            feedbackManager.onButtonClick(it)
            findNavController().navigate(R.id.action_gameFragment_to_settingsFragment)
        }

        binding.overlayGameStatus.btnRestartGameOver.setOnClickListener {
            feedbackManager.onButtonClick(it)
            restartLevel()
        }
        
        binding.overlayGameStatus.btnRestartComplete.setOnClickListener {
            feedbackManager.onButtonClick(it)
            restartLevel()
        }
        
        binding.overlayGameStatus.btnNextLevel.setOnClickListener {
            feedbackManager.onButtonClick(it)
            loadNextLevel()
        }
    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.tvArrowCount.text = state.arrowsLeft.toString()
                binding.gameBoard.setHintArrowId(state.hintArrowId)
                binding.gameBoard.setUndoEnabled(state.canUndo)
                
                if (state.combo >= 2) {
                    binding.tvCombo.visibility = View.VISIBLE
                    binding.tvCombo.text = getString(R.string.combo, state.combo)
                } else {
                    binding.tvCombo.visibility = View.GONE
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            progressRepository.hintsLeft.collectLatest { left ->
                binding.tvHintCount.text = left.toString()
            }
        }
    }

    private fun setupTutorial() {
        viewLifecycleOwner.lifecycleScope.launch {
            val done = progressRepository.isTutorialDone.first()
            if (!done && gameEngine.currentLevel?.id == 1) {
                binding.overlayTutorial.tutorialOverlay.visibility = View.VISIBLE
                binding.overlayTutorial.btnTutorialOk.setOnClickListener {
                    feedbackManager.onButtonClick(it)
                    binding.overlayTutorial.tutorialOverlay.visibility = View.GONE
                    lifecycleScope.launch { progressRepository.setTutorialDone() }
                }
            } else {
                binding.overlayTutorial.tutorialOverlay.visibility = View.GONE
            }
        }
    }

    private fun resetOverlays() {
        binding.overlayGameStatus.overlayGameOver.visibility = View.GONE
        binding.overlayGameStatus.overlayGameOver.alpha = 0f
        binding.overlayGameStatus.overlayLevelComplete.visibility = View.GONE
        binding.overlayGameStatus.overlayLevelComplete.alpha = 0f
    }

    private fun restartLevel() {
        gameEngine.currentLevel?.let {
            gameEngine.loadLevel(it)
        }
        hideOverlays()
    }

    private fun loadNextLevel() {
        val nextLevelId = viewModel.nextLevelId()
        if (nextLevelId != null) {
            val nextLevel = LevelRepository.getLevel(nextLevelId)
            if (nextLevel != null) {
                AdsManager.showInterstitial(requireActivity()) {
                    gameEngine.loadLevel(nextLevel)
                    hideOverlays()
                }
                return
            }
        }
        findNavController().popBackStack(R.id.menuFragment, false)
    }

    private fun updateLivesUI(lives: Int) {
        val hearts = listOf(binding.heart1, binding.heart2, binding.heart3)
        
        // Ensure index is within bounds (0-2)
        val heartIndexToUpdate = lives.coerceIn(0, 2)
        
        // 1. If lives decreased, animate the heart at the 'lives' index (which just became empty)
        if (lives < previousLives && lives >= 0) {
            val heartToEmpty = hearts[heartIndexToUpdate]
            heartToEmpty.animate()
                .scaleX(1.4f)
                .scaleY(1.4f)
                .alpha(0.5f)
                .setDuration(250)
                .withEndAction {
                    heartToEmpty.setImageResource(R.drawable.ic_heart_empty)
                    heartToEmpty.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(150)
                        .start()
                }
                .start()
        } 
        // 2. If lives increased or reset, refresh all icons
        else {
            hearts.forEachIndexed { index, heart ->
                heart.animate().cancel()
                heart.scaleX = 1.0f
                heart.scaleY = 1.0f
                heart.alpha = 1.0f
                if (index < lives) {
                    heart.setImageResource(R.drawable.ic_heart)
                } else {
                    heart.setImageResource(R.drawable.ic_heart_empty)
                }
            }
        }
        
        previousLives = lives
    }

    override fun onArrowExit(arrow: Arrow) {
        feedbackManager.onMoveSuccess(binding.gameBoard)
        viewModel.onMoveSuccess()
    }

    override fun onArrowBlocked(arrow: Arrow) {
        feedbackManager.onMoveBlocked(binding.gameBoard)
        viewModel.onMoveBlocked()
    }

    override fun onLivesChanged(lives: Int) {
        if (lives < previousLives) {
            feedbackManager.onLifeLost(binding.gameBoard)
        }
        updateLivesUI(lives)
        viewModel.onLives(lives)
    }

    override fun onGameStateChanged(state: GameState, stars: Int) {
        viewModel.onState(state, stars)
        when (state) {
            GameState.GAME_OVER -> {
                feedbackManager.onGameOver(binding.gameBoard)
                binding.root.postDelayed({
                    if (_binding != null) showOverlay(binding.overlayGameStatus.overlayGameOver)
                }, 500)
            }
            GameState.LEVEL_COMPLETE -> {
                feedbackManager.onLevelComplete(binding.gameBoard)
                AdsManager.onLevelCompleted()
                val isDaily = arguments?.getBoolean("isDaily") ?: false
                if (isDaily) {
                    lifecycleScope.launch { progressRepository.setDailyPuzzleCompleted() }
                } else {
                    viewModel.saveProgress(stars)
                }
                binding.root.postDelayed({
                    if (_binding != null) showLevelComplete(stars)
                }, 900) // Slightly longer to allow dot fade-out
            }
            GameState.PLAYING -> hideOverlays()
        }
    }

    private fun showOverlay(overlay: View) {
        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f
        overlay.scaleX = 0.8f
        overlay.scaleY = 0.8f
        overlay.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    private fun hideOverlays() {
        binding.overlayGameStatus.overlayGameOver.animate().alpha(0f).setDuration(200).withEndAction { 
            binding.overlayGameStatus.overlayGameOver.visibility = View.GONE 
        }
        binding.overlayGameStatus.overlayLevelComplete.animate().alpha(0f).setDuration(200).withEndAction { 
            binding.overlayGameStatus.overlayLevelComplete.visibility = View.GONE 
        }
    }

    private fun showLevelComplete(stars: Int) {
        val level = gameEngine.currentLevel ?: return
        val isDaily = arguments?.getBoolean("isDaily") ?: false
        
        if (isDaily) {
            binding.overlayGameStatus.tvCompleteInfo.text = getString(R.string.daily_puzzle_completed)
            binding.overlayGameStatus.btnNextLevel.text = getString(R.string.back)
            binding.overlayGameStatus.btnNextLevel.setOnClickListener {
                feedbackManager.onButtonClick(it)
                findNavController().navigateUp()
            }
        } else {
            val difficultyText = when (level.difficulty?.name) {
                "HARD" -> getString(R.string.hard)
                "MEDIUM" -> getString(R.string.medium)
                else -> getString(R.string.easy)
            }
            binding.overlayGameStatus.tvCompleteInfo.text = getString(R.string.level_info, level.id, difficultyText)
            val nextLevelId = viewModel.nextLevelId()
            binding.overlayGameStatus.btnNextLevel.text = if (nextLevelId != null) {
                getString(R.string.next_level)
            } else {
                getString(R.string.all_levels_completed)
            }
            binding.overlayGameStatus.btnNextLevel.setOnClickListener {
                feedbackManager.onButtonClick(it)
                loadNextLevel()
            }
        }
        
        val starsText = StringBuilder()
        for (i in 0 until 3) {
            starsText.append(if (i < stars) "⭐" else "☆")
        }
        binding.overlayGameStatus.tvStars.text = starsText.toString()
        
        showOverlay(binding.overlayGameStatus.overlayLevelComplete)
        animateStarsEffect()
    }

    private fun animateStarsEffect() {
        binding.overlayGameStatus.tvStars.scaleX = 0f
        binding.overlayGameStatus.tvStars.scaleY = 0f
        binding.overlayGameStatus.tvStars.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(400)
            .withEndAction {
                binding.overlayGameStatus.tvStars.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    override fun onLevelLoaded(level: Level, board: Board) {
        val isDaily = arguments?.getBoolean("isDaily") ?: false
        if (isDaily) {
            binding.tvLevel.text = getString(R.string.daily_puzzle)
        } else {
            binding.tvLevel.text = getString(R.string.level_number, level.id)
        }
        
        val difficultyRes = when (level.difficulty?.name) {
            "HARD" -> R.string.hard
            "MEDIUM" -> R.string.medium
            else -> R.string.easy
        }
        binding.tvDifficulty.text = getString(difficultyRes)
        
        previousLives = 3
        updateLivesUI(3)
        hideOverlays()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.bannerContainer.removeAllViews()
        gameEngine.removeListener(this)
        gameEngine.removeListener(binding.gameBoard)
        feedbackManager.release()
        _binding = null
    }
}
