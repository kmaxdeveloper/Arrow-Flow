package uz.kmax.arrowflow.model

data class Level(
    val id: Int,
    val size: Int,
    val difficulty: Difficulty,
    val arrows: List<Arrow>
)
