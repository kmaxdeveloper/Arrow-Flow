package uz.kmax.arrowflow.model

import com.google.gson.annotations.SerializedName

enum class Difficulty {
    @SerializedName("Easy", alternate = ["EASY"])
    EASY,
    @SerializedName("Medium", alternate = ["MEDIUM"])
    MEDIUM,
    @SerializedName("Hard", alternate = ["HARD"])
    HARD
}
