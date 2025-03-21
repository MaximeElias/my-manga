package iutlens.android.mymangalist.model

data class Anime(
    val id: Int,
    val title: String,
    val state: String,
    val seasons: List<Season>,
    val coverUrl: String
)