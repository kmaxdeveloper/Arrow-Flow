package uz.kmax.arrowflow.model

class Board(val size: Int = 8) {
    private val arrows = mutableListOf<Arrow>()

    constructor(level: Level) : this(level.size) {
        level.arrows.forEach { addArrow(it.copy()) }
    }

    fun addArrow(arrow: Arrow) {
        if (arrow.points.all { it.first in 0 until size && it.second in 0 until size }) {
            arrows.add(arrow)
        }
    }

    fun getArrowAt(row: Int, col: Int): Arrow? {
        return arrows.find { arrow -> 
            arrow.points.any { it.first == row && it.second == col }
        }
    }

    fun isCellOccupied(row: Int, col: Int): Boolean {
        return getArrowAt(row, col) != null
    }

    fun removeArrow(arrow: Arrow) {
        arrows.remove(arrow)
    }

    fun restoreArrow(arrow: Arrow) {
        if (arrows.none { it.id == arrow.id }) arrows.add(arrow.copy())
    }

    fun getArrows(): List<Arrow> = arrows

    fun isEmpty(): Boolean = arrows.isEmpty()
}
