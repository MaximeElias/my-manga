package iutlens.android.mymangalist.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import iutlens.android.mymangalist.model.Anime
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class AnimeDatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "AnimeDatabase.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_ANIMES = "animes"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_STATE = "state"
        const val COLUMN_COVER_URL = "coverUrl"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE $TABLE_ANIMES ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_TITLE TEXT, $COLUMN_STATE TEXT, $COLUMN_COVER_URL TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_ANIMES")
        onCreate(db)
    }

    fun addAnime(title: String, state: String, coverUrl: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_STATE, state)
            put(COLUMN_COVER_URL, coverUrl)
        }
        val result = db.insert(TABLE_ANIMES, null, values)
        db.close()
        return result
    }

    fun getAnimes(): List<Anime> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ANIMES ORDER BY $COLUMN_TITLE ASC", null)
        val animes = mutableListOf<Anime>()

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
            val state = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATE))
            val coverUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COVER_URL))

            animes.add(Anime(id, title, state, emptyList(), coverUrl))
        }
        cursor.close()
        db.close()

        return animes
    }

    fun updateAnime(id: Int, title: String, state: String, coverUrl: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_STATE, state)
            put(COLUMN_COVER_URL, coverUrl)
        }
        db.update(TABLE_ANIMES, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun deleteAnime(id: Int) {
        val db = writableDatabase
        db.delete(TABLE_ANIMES, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun getTotalAnimes(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_ANIMES", null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        db.close()
        return count
    }

    fun updateJSONFile() {
        val animes = getAnimes()
        val jsonArray = JSONArray()

        for (anime in animes) {
            val jsonObject = JSONObject()
            jsonObject.put("id", anime.id)
            jsonObject.put("title", anime.title)
            jsonObject.put("state", anime.state)
            jsonObject.put("coverUrl", anime.coverUrl)

            // Ajoutez les saisons si nécessaire
            val seasonsArray = JSONArray()
            for (season in anime.seasons) {
                val seasonObject = JSONObject()
                seasonObject.put("seasonNumber", season.seasonNumber)
                seasonObject.put("episodesWatched", season.episodesWatched)
                seasonObject.put("totalEpisodes", season.totalEpisodes)
                seasonsArray.put(seasonObject)
            }
            jsonObject.put("seasons", seasonsArray)

            jsonArray.put(jsonObject)
        }

        try {
            val fileOutputStream = context.openFileOutput("animes.json", Context.MODE_PRIVATE)
            fileOutputStream.write(jsonArray.toString().toByteArray())
            fileOutputStream.close()
            Log.d("AnimeDatabaseHelper", "animes.json updated successfully")
        } catch (e: IOException) {
            Log.e("AnimeDatabaseHelper", "Error updating animes.json: ${e.message}")
        }
    }
}