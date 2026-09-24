package uz.kmax.arrowflow.logic

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uz.kmax.arrowflow.model.Level
import java.io.InputStreamReader

/**
 * Level Repository that loads levels from assets/levels.json.
 * Falls back to Generator if JSON is missing or for levels beyond the predefined set.
 */
object LevelRepository {
    
    private const val MAX_LEVELS = 3000
    private val loadedLevels: MutableMap<Int, Level> = mutableMapOf()
    private val loadedChunks: MutableSet<Int> = mutableSetOf()
    private val gson = Gson()
    private var appContext: Context? = null

    /**
     * Call this from MainActivity or Application class to initialize.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        // Pre-load the first chunk
        loadChunkForLevel(1)
    }

    private fun loadChunkForLevel(id: Int) {
        val context = appContext ?: return
        val chunkIndex = ((id - 1) / 100) * 100 + 1
        val chunkEnd = chunkIndex + 99
        
        if (loadedChunks.contains(chunkIndex)) return

        val fileName = "levels/levels_${chunkIndex}_${chunkEnd}.json"
        try {
            context.assets.open(fileName).use { inputStream ->
                val reader = InputStreamReader(inputStream)
                val type = object : TypeToken<List<Level>>() {}.type
                val levelList: List<Level> = gson.fromJson(reader, type)
                levelList.forEach { loadedLevels[it.id] = it }
                loadedChunks.add(chunkIndex)
            }
        } catch (e: Exception) {
            loadedChunks.add(chunkIndex)
        }
    }

    fun getLevel(id: Int): Level? {
        if (id <= 0 || id > MAX_LEVELS) return null
        
        // Ensure the chunk is loaded
        loadChunkForLevel(id)
        
        // Try to get from JSON cache first
        return loadedLevels[id] ?: LevelGenerator.generate(id)
    }

    fun getNextLevel(currentId: Int): Level? = getLevel(currentId + 1)

    fun getTotalLevels(): Int = MAX_LEVELS

    fun getAllPredefinedLevels(): List<Level> {
        val context = appContext ?: return emptyList()
        try {
            context.assets.list("levels")?.forEach { fileName ->
                if (fileName.endsWith(".json")) {
                    val parts = fileName.removeSuffix(".json").split("_")
                    if (parts.size >= 2) {
                        val chunkStart = parts[1].toIntOrNull()
                        if (chunkStart != null) loadChunkForLevel(chunkStart)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return loadedLevels.values.toList().sortedBy { it.id }
    }
}
