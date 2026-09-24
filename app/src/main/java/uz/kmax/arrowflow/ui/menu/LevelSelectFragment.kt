package uz.kmax.arrowflow.ui.menu

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import uz.kmax.arrowflow.R
import uz.kmax.arrowflow.databinding.FragmentLevelSelectBinding
import uz.kmax.arrowflow.logic.AdsManager
import uz.kmax.arrowflow.logic.FeedbackManager
import uz.kmax.arrowflow.logic.LevelRepository
import uz.kmax.arrowflow.logic.ProgressRepository
import uz.kmax.arrowflow.logic.dataStore
import uz.kmax.base.fragment.BaseFragment

class LevelSelectFragment : BaseFragment(R.layout.fragment_level_select) {
    private var _binding: FragmentLevelSelectBinding? = null
    private val binding get() = _binding!!
    private lateinit var progressRepository: ProgressRepository
    private lateinit var feedbackManager: FeedbackManager
    override fun onViewCreated() {
        _binding = FragmentLevelSelectBinding.bind(layout)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, systemBars.bottom)
            insets
        }
        
        progressRepository = ProgressRepository(requireContext())
        feedbackManager = FeedbackManager(requireContext(), progressRepository)
        
        binding.btnBack.setOnClickListener {
            feedbackManager.onButtonClick(it)
            findNavController().navigateUp()
        }
        
        setupLevelList()
        
        AdsManager.showNativeAd(binding.nativeAdContainer, requireActivity(), R.layout.ad_unified)
    }
    private fun setupLevelList() {
        lifecycleScope.launch {
            val allLevels = LevelRepository.getAllPredefinedLevels()
            val highestUnlocked = progressRepository.highestUnlockedLevel.first()
            val preferences = requireContext().dataStore.data.first()
            val items = allLevels.map { level ->
                val stars = preferences[ProgressRepository.getStarKey(level.id)] ?: 0
                LevelSelectItem(level = level, bestStars = stars, isUnlocked = level.id <= highestUnlocked)
            }
            val totalStars = items.sumOf { it.bestStars }
            binding.tvProgress.text = "Level $highestUnlocked / ${allLevels.size} - $totalStars stars"
            binding.progressLevels.max = allLevels.size
            binding.progressLevels.progress = (highestUnlocked - 1).coerceIn(0, allLevels.size)
            binding.rvLevels.adapter = LevelSelectAdapter(items) { level ->
                feedbackManager.onButtonClick(binding.rvLevels)
                val bundle = Bundle().apply { putInt("levelId", level.id) }
                findNavController().navigate(R.id.action_levelSelectFragment_to_gameFragment, bundle)
            }
            val targetPosition = (highestUnlocked - 1).coerceIn(0, (allLevels.size - 1).coerceAtLeast(0))
            binding.rvLevels.post {
                if (_binding != null) {
                    binding.rvLevels.scrollToPosition(targetPosition)
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        feedbackManager.release()
        _binding = null
    }
}
