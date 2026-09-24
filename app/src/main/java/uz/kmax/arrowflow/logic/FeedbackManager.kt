package uz.kmax.arrowflow.logic

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FeedbackManager(private val context: Context, private val repository: ProgressRepository) {

    private val audioManager = GameAudioManager(context)
    private var vibrationEnabled = true
    private var soundEnabled = true
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            repository.isVibrationEnabled.collect { vibrationEnabled = it }
        }
        scope.launch {
            repository.isSoundEnabled.collect { soundEnabled = it }
        }
    }

    fun onMoveSuccess(view: View) {
        if (vibrationEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
        if (soundEnabled) {
            audioManager.play(GameAudioManager.SoundType.MOVE)
        }
    }

    fun onMoveBlocked(view: View) {
        if (vibrationEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
        if (soundEnabled) {
            audioManager.play(GameAudioManager.SoundType.BLOCKED)
        }
    }

    fun onLifeLost(view: View) {
        if (vibrationEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        if (soundEnabled) {
            audioManager.play(GameAudioManager.SoundType.LIFE_LOST)
        }
    }

    fun onLevelComplete(view: View) {
        if (vibrationEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        if (soundEnabled) {
            audioManager.play(GameAudioManager.SoundType.LEVEL_COMPLETE)
        }
    }

    fun onGameOver(view: View) {
        if (vibrationEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
        if (soundEnabled) {
            audioManager.play(GameAudioManager.SoundType.GAME_OVER)
        }
    }

    fun onButtonClick(view: View) {
        if (vibrationEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (soundEnabled) {
            audioManager.play(GameAudioManager.SoundType.CLICK)
        }
    }

    fun release() {
        scope.cancel()
        audioManager.release()
    }
}
