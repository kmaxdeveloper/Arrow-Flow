package uz.kmax.arrowflow

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.kmax.arrowflow.model.Arrow
import uz.kmax.arrowflow.model.Board
import uz.kmax.arrowflow.model.Direction
import uz.kmax.arrowflow.model.Level
import java.io.File

class LevelValidationTest {

    @Test
    fun validateAll3000LevelsSolvable() {
        val gson = Gson()
        val type = object : TypeToken<List<Level>>() {}.type

        val assetsDir = File("src/main/assets/levels").takeIf { it.exists() }
            ?: File("app/src/main/assets/levels")

        val jsonFiles = assetsDir.listFiles { _, name -> name.endsWith(".json") }?.sortedBy { it.name }
        assertTrue("Levels directory should contain json files", !jsonFiles.isNullOrEmpty())

        var totalTested = 0
        jsonFiles!!.forEach { file ->
            val json = file.readText()
            val levelList: List<Level> = gson.fromJson(json, type)
            levelList.forEach { level ->
                totalTested++
                assertTrue("Level ${level.id} should be solvable", isSolvable(level))
            }
        }

        assertEquals("Should test exactly 3000 levels", 3000, totalTested)
    }

    private fun isSolvable(level: Level): Boolean {
        return solveRecursive(Board(level))
    }

    private fun solveRecursive(board: Board): Boolean {
        if (board.isEmpty()) return true

        val arrows = board.getArrows().toList()
        for (arrow in arrows) {
            if (isPathClear(board, arrow)) {
                val nextBoard = copyBoard(board)
                val arrowInNext = nextBoard.getArrowAt(arrow.points.first().first, arrow.points.first().second)!!
                nextBoard.removeArrow(arrowInNext)

                if (solveRecursive(nextBoard)) return true
            }
        }
        return false
    }

    private fun isPathClear(board: Board, arrow: Arrow): Boolean {
        val dr = when (arrow.direction) {
            Direction.UP -> -1
            Direction.DOWN -> 1
            else -> 0
        }
        val dc = when (arrow.direction) {
            Direction.LEFT -> -1
            Direction.RIGHT -> 1
            else -> 0
        }

        val head = arrow.points.lastOrNull() ?: return true
        var currR = head.first + dr
        var currC = head.second + dc

        while (currR in 0 until board.size && currC in 0 until board.size) {
            val other = board.getArrowAt(currR, currC)
            if (other != null && other.id != arrow.id) return false
            currR += dr
            currC += dc
        }

        return true
    }

    private fun copyBoard(board: Board): Board {
        val newBoard = Board(board.size)
        board.getArrows().forEach { newBoard.addArrow(it.copy()) }
        return newBoard
    }
}
