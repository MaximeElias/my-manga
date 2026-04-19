package iutlens.android.mymangalist.model

import android.os.Parcel
import android.os.Parcelable

data class Serie(
    val id: Int,
    val title: String,
    val coverUrl: String,
    val status: String = "",   // "En cours", "Terminé" — vient du JSON
    val volumes: MutableList<Volume>
) : Parcelable {

    // ─── Propriétés calculées ─────────────────────────────────────────────────

    val volumeIntegral: Int
        get() = volumes.size

    val volumesOwned: Int
        get() = volumes.count { it.isOwned }

    val volumesRead: Int
        get() = volumes.count { it.isRead }

    val chaptersRead: Int
        get() = volumes.sumOf { v -> v.chapters.count { it.isRead } }

    val totalChapters: Int
        get() = volumes.sumOf { it.chapters.size }

    val lastChapterRead: Int
        get() = volumes.flatMap { it.chapters }
            .filter { it.isRead }
            .maxOfOrNull { it.number } ?: 0

    // ─── Parcelable ───────────────────────────────────────────────────────────

    constructor(parcel: Parcel) : this(
        id       = parcel.readInt(),
        title    = parcel.readString() ?: "",
        coverUrl = parcel.readString() ?: "",
        status   = parcel.readString() ?: "",
        volumes  = (parcel.createTypedArrayList(Volume.CREATOR) ?: arrayListOf()).toMutableList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(coverUrl)
        parcel.writeString(status)
        parcel.writeTypedList(volumes)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Serie> {
        override fun createFromParcel(parcel: Parcel) = Serie(parcel)
        override fun newArray(size: Int): Array<Serie?> = arrayOfNulls(size)
    }
}