package iutlens.android.mymangalist.model

import android.os.Parcel
import android.os.Parcelable

data class Volume(
    val number: Int,
    val chapters: MutableList<Chapter>,
    var isOwned: Boolean = false,
    var isRead: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.createTypedArrayList(Chapter.CREATOR) ?: mutableListOf(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(number)
        parcel.writeTypedList(chapters)
        parcel.writeByte(if (isOwned) 1 else 0)
        parcel.writeByte(if (isRead) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Volume> {
        override fun createFromParcel(parcel: Parcel): Volume {
            return Volume(parcel)
        }

        override fun newArray(size: Int): Array<Volume?> {
            return arrayOfNulls(size)
        }
    }
}
