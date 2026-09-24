package uz.kmax.arrowflow.logic

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import uz.kmax.arrowflow.R

class GameAudioManager(private val context: Context) {

    enum class SoundType {
        MOVE, BLOCKED, LIFE_LOST, LEVEL_COMPLETE, GAME_OVER, CLICK
    }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundMap = mutableMapOf<SoundType, Int>()

    init {
        loadSounds()
    }

    private fun loadSounds() {
        // We use try-catch or check existence to avoid crashes if files aren't added yet
        loadSound(SoundType.MOVE, "move")
        loadSound(SoundType.BLOCKED, "blocked")
        loadSound(SoundType.LIFE_LOST, "life_lost")
        loadSound(SoundType.LEVEL_COMPLETE, "level_complete")
        loadSound(SoundType.GAME_OVER, "game_over")
        loadSound(SoundType.CLICK, "click")
    }

    private fun loadSound(type: SoundType, name: String) {
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        if (resId != 0) {
            soundMap[type] = soundPool.load(context, resId, 1)
        } else {
            Log.i("GameAudioManager", "Sound resource '$name' not found in res/raw. Place sound files there to enable gameplay audio.")
        }
    }

    private var isReleased = false

    fun play(type: SoundType) {
        if (isReleased) return
        val soundId = soundMap[type]
        if (soundId != null && soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        if (!isReleased) {
            isReleased = true
            soundPool.release()
        }
    }
}
