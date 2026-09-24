package uz.kmax.arrowflow.ui.menu

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import uz.kmax.arrowflow.R
import uz.kmax.base.fragment.BaseFragment
import uz.kmax.arrowflow.databinding.FragmentMenuBinding
import uz.kmax.arrowflow.logic.AdsManager
import uz.kmax.arrowflow.logic.FeedbackManager
import uz.kmax.arrowflow.logic.LevelRepository
import uz.kmax.arrowflow.logic.ProgressRepository
import java.util.Calendar

class MenuFragment : BaseFragment(R.layout.fragment_menu) {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!
    private lateinit var progressRepository: ProgressRepository
    private lateinit var feedbackManager: FeedbackManager

    override fun onViewCreated() {
        _binding = FragmentMenuBinding.bind(layout)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, systemBars.bottom)
            insets
        }
        
        progressRepository = ProgressRepository(requireContext())
        feedbackManager = FeedbackManager(requireContext(), progressRepository)
        
        setupListeners()
        loadStats()
        applyJuicyTouch(binding.btnStart, binding.btnDailyPuzzle, binding.btnLevelSelect, binding.btnSettings)
        
        AdsManager.showBanner(binding.bannerContainer, requireActivity())
    }

    private fun applyJuicyTouch(vararg views: View) {
        views.forEach { view ->
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).start()
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
        binding.btnStart.setOnClickListener {
            feedbackManager.onButtonClick(it)
            lifecycleScope.launch {
                val highest = progressRepository.highestUnlockedLevel.first()
                val bundle = Bundle().apply {
                    putInt("levelId", highest)
                }
                findNavController().navigate(R.id.action_menuFragment_to_gameFragment, bundle)
            }
        }

        binding.btnDailyPuzzle.setOnClickListener {
            feedbackManager.onButtonClick(it)
            lifecycleScope.launch {
                val today = Calendar.getInstance()
                val seed = today.get(Calendar.YEAR) * 10000 + (today.get(Calendar.MONTH) + 1) * 100 + today.get(
                    Calendar.DAY_OF_MONTH)
                val dailyLevelId = 450 + (seed % 250) + 1
                val bundle = Bundle().apply {
                    putInt("levelId", dailyLevelId)
                    putBoolean("isDaily", true)
                }
                findNavController().navigate(R.id.action_menuFragment_to_gameFragment, bundle)
            }
        }

        binding.btnLevelSelect.setOnClickListener {
            feedbackManager.onButtonClick(it)
            try {
                findNavController().navigate(R.id.levelSelectFragment)
            } catch (e: Exception) {
                // Fallback navigation if action ID fails
                findNavController().navigate(R.id.action_menuFragment_to_levelSelectFragment)
            }
        }

        binding.btnSettings.setOnClickListener {
            feedbackManager.onButtonClick(it)
            findNavController().navigate(R.id.action_menuFragment_to_settingsFragment)
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val highest = progressRepository.highestUnlockedLevel.first()
            val completed = (highest - 1).coerceAtLeast(0)
            val total = LevelRepository.getTotalLevels()
            val streak = progressRepository.currentStreak.first()
            val pct = if (total > 0) (completed * 100 / total) else 0
            val fire = if (streak > 0) " 🔥$streak" else ""
            
            binding.tvStats.text = "⭐ Level $highest  •  $completed/$total ($pct%)$fire"
            binding.btnStart.text = if (highest > 1) "Continue Level $highest" else "Start New Adventure"

            val isDailyDone = progressRepository.isDailyPuzzleCompletedToday.first()
            if (isDailyDone) {
                binding.btnDailyPuzzle.text = getString(R.string.daily_puzzle_completed)
                binding.btnDailyPuzzle.alpha = 0.8f
                binding.btnDailyPuzzle.setIconResource(android.R.drawable.checkbox_on_background)
            } else {
                binding.btnDailyPuzzle.text = getString(R.string.daily_puzzle)
                binding.btnDailyPuzzle.alpha = 1.0f
                binding.btnDailyPuzzle.setIconResource(android.R.drawable.ic_menu_my_calendar)
            }
            
            // Subtle pulse animation for the logo
            binding.ivLogo.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(1500)
                .withEndAction {
                    if (_binding != null) {
                        binding.ivLogo.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(1500)
                            .start()
                    }
                }
                .start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        feedbackManager.release()
        _binding = null
    }
}
