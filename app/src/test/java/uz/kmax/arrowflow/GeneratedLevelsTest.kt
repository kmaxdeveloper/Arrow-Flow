package uz.kmax.arrowflow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.kmax.arrowflow.logic.LevelGenerator
import uz.kmax.arrowflow.logic.LevelRepository

class GeneratedLevelsTest {

    @Test
    fun generatedLevels_areSolvable_andUnique() {
        val seen = mutableSetOf<String>()
        val totalLevels = LevelRepository.getTotalLevels()
        for (id in 4..totalLevels) {
            val level = LevelGenerator.generate(id)
            assertEquals(id, level.id)
            assertTrue("Level $id kamida 4 o'qqa ega bo'lishi kerak", level.arrows.size >= 4)
            // chegara ichida va takrorlanmas hujayralar
            val cells = mutableSetOf<Pair<Int, Int>>()
            level.arrows.forEach { arrow ->
                arrow.points.forEach { p ->
                    assertTrue("Level $id hujayra chegaradan tashqarida: $p", (p.first in 0 until level.size) && (p.second in 0 until level.size))
                    assertTrue("Level $id da takror hujayra: $p", cells.add(p))
                }
            }
            // deterministik: bir xil id bir xil level beradi
            val again = LevelGenerator.generate(id)
            assertEquals(level, again)
            // xilma-xillik
            seen.add(level.arrows.joinToString { "${it.direction}:${it.points}" })
        }
        assertTrue("Generatsiya qilingan levellar bir-biridan farq qilishi kerak", seen.size > 10)
    }
}
