package iutlens.android.mymangalist.model

data class Anime(
    val title: String,
    val status: String,
    val seasons: List<Season>
)