package uz.kmax.arrowflow.ui.menu

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import uz.kmax.arrowflow.R
import uz.kmax.arrowflow.databinding.FragmentSettingsBinding
import uz.kmax.arrowflow.logic.FeedbackManager
import uz.kmax.arrowflow.logic.ProgressRepository
import uz.kmax.base.fragment.BaseFragment

class SettingsFragment : BaseFragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: ProgressRepository
    private lateinit var feedbackManager: FeedbackManager

    override fun onViewCreated() {
        _binding = FragmentSettingsBinding.bind(layout)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, systemBars.bottom)
            insets
        }
        
        repository = ProgressRepository(requireContext())
        feedbackManager = FeedbackManager(requireContext(), repository)

        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            feedbackManager.onButtonClick(it)
            findNavController().navigateUp()
        }

        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            feedbackManager.onButtonClick(binding.switchSound)
            lifecycleScope.launch {
                repository.setSoundEnabled(isChecked)
            }
        }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            feedbackManager.onButtonClick(binding.switchVibration)
            lifecycleScope.launch {
                repository.setVibrationEnabled(isChecked)
            }
        }

        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val version = pInfo.versionName
            binding.tvVersion.text = "Version $version"
        } catch (e: Exception) {
            binding.tvVersion.text = "Version 1.0"
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            val soundEnabled = repository.isSoundEnabled.first()
            val vibrationEnabled = repository.isVibrationEnabled.first()
            
            binding.switchSound.isChecked = soundEnabled
            binding.switchVibration.isChecked = vibrationEnabled
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        feedbackManager.release()
        _binding = null
    }
}
