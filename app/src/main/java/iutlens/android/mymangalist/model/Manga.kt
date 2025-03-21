package iutlens.android.mymangalist.model

data class Manga(
    val id: Int,
    var title: String,
    var volumeIntegral: Int,
    var volumesOwned: Int,
    var volumesRead: Int,
    var chapitre: Int,
    var state: String,
    val coverUrl: String
)
