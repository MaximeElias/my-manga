package iutlens.android.mymangalist.model

import android.os.Parcel
import android.os.Parcelable

data class Chapter(
    val number: Int,
    val name: String = "",
    var isRead: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(number)
        parcel.writeString(name)
        parcel.writeByte(if (isRead) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Chapter> {
        override fun createFromParcel(parcel: Parcel): Chapter {
            return Chapter(parcel)
        }

        override fun newArray(size: Int): Array<Chapter?> {
            return arrayOfNulls(size)
        }
    }
}
