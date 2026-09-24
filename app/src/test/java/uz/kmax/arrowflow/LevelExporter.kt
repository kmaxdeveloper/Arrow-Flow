package uz.kmax.arrowflow

import com.google.gson.GsonBuilder
import org.junit.Test
import uz.kmax.arrowflow.logic.LevelGenerator
import java.io.File

class LevelExporter {
    @Test
    fun exportLevelsToJson() {
        val levels = (1..100).map { id ->
            LevelGenerator.generate(id)
        }
        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(levels)
        println(json)
        
        // Write to a known path if possible, or just print it.
        // Since I'm the AI, I'll just print it and then use write_file tool.
    }
}
