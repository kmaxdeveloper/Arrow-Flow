package uz.kmax.arrowflow.model

data class Arrow(
    val id: Int,
    val points: List<Pair<Int, Int>>, // Egallagan barcha kataklari (quyruqdan boshgacha)
    val direction: Direction        // Harakat yo'nalishi
)
