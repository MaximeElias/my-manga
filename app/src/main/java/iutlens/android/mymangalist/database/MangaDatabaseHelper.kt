package iutlens.android.mymangalist.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import iutlens.android.mymangalist.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader

class MangaDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val appContext = context

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE IF NOT EXISTS mangas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                volumeIntegral INTEGER DEFAULT 0,
                volumesOwned INTEGER DEFAULT 0,
                volumesRead INTEGER DEFAULT 0,
                chapitre INTEGER DEFAULT 0,
                state TEXT DEFAULT "En Cours",
                coverUrl TEXT DEFAULT "default_cover.jpg"
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    // Fonction pour obtenir le total des tomes possédés
    fun getTotalTomesPossedes(): Int {
        val db = this.readableDatabase
        val query = "SELECT SUM($COLUMN_VOLUMES_OWNED) FROM $TABLE_MANGAS"
        val cursor = db.rawQuery(query, null)

        var totalTomes = 0
        if (cursor.moveToFirst()) {
            totalTomes = cursor.getInt(0)
        }
        cursor.close()
        return totalTomes
    }

    // Fonction pour obtenir le total des tomes lus
    fun getTotalTomesLus(): Int {
        val db = this.readableDatabase
        val query = "SELECT SUM($COLUMN_VOLUMES_READ) FROM $TABLE_MANGAS"
        val cursor = db.rawQuery(query, null)

        var totalTomesLus = 0
        if (cursor.moveToFirst()) {
            totalTomesLus = cursor.getInt(0)
        }
        cursor.close()
        return totalTomesLus
    }

    // Fonction pour obtenir le total des séries
    fun getTotalSeries(): Int {
        val db = this.readableDatabase
        val query = "SELECT COUNT(DISTINCT $COLUMN_TITLE) FROM $TABLE_MANGAS"
        val cursor = db.rawQuery(query, null)

        var totalSeries = 0
        if (cursor.moveToFirst()) {
            totalSeries = cursor.getInt(0)
        }
        cursor.close()
        return totalSeries
    }

    fun updateManga(
        mangaId: Int,
        newTitle: String,
        newVolumeIntegral: Int,
        newVolumesOwned: Int,
        newVolumesRead: Int,
        newState: String,
        newChapitre: Int,
        coverUrl: String
    ) {
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_TITLE, newTitle)
            put(COLUMN_VOLUME_INTEGRAL, newVolumeIntegral)
            put(COLUMN_VOLUMES_OWNED, newVolumesOwned)
            put(COLUMN_VOLUMES_READ, newVolumesRead)
            put(COLUMN_STATE, newState)
            put(COLUMN_CHAPITRE, newChapitre)
            put(COLUMN_URL, coverUrl)
        }

        db.update(TABLE_MANGAS, contentValues, "id = ?", arrayOf(mangaId.toString()))
        db.close()

        // Mise à jour du fichier JSON (si nécessaire)
        updateJSONFile()

        // Mise à jour de l'UI, on affiche l'image avec l'URL récupérée
        (appContext as? MainActivity)?.updateTotalUI()
    }

    // Méthode pour ajouter un manga à la base de données
    fun addManga(title: String, volumeIntegral: Int, volumeOwned: Int, volumesRead: Int, chapitre: Int, state: String, coverUrl: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", title)
            put("volumeIntegral", volumeIntegral)
            put("volumesOwned", volumeOwned)
            put("volumesRead", volumesRead)
            put("chapitre", chapitre)
            put("state", state)
            put("coverUrl", coverUrl)
        }

        updateJSONFile()

        // Insérer les valeurs dans la table mangas
        return db.insertOrThrow("mangas", null, values)
    }

    fun deleteManga(mangaId: Int) {
        val db = writableDatabase
        db.delete("mangas", "id = ?", arrayOf(mangaId.toString()))
        db.close()
    }

    // Fonction pour mettre à jour le fichier JSON avec les données de la BDD
    fun updateJSONFile() {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_MANGAS ORDER BY $COLUMN_TITLE ASC"
        val cursor = db.rawQuery(query, null)

        val mangasList = JSONArray() // Crée un JSONArray pour contenir tous les mangas

        // Vérifier si les colonnes existent
        val titleIndex = cursor.getColumnIndex(COLUMN_TITLE)
        val volumeIntegralIndex = cursor.getColumnIndex(COLUMN_VOLUME_INTEGRAL)
        val volumesOwnedIndex = cursor.getColumnIndex(COLUMN_VOLUMES_OWNED)
        val volumesReadIndex = cursor.getColumnIndex(COLUMN_VOLUMES_READ)
        val stateIndex = cursor.getColumnIndex(COLUMN_STATE)
        val chapitreIndex = cursor.getColumnIndex(COLUMN_CHAPITRE)
        val coverUrlIndex = cursor.getColumnIndex(COLUMN_URL)

        if (cursor.moveToFirst()) {
            do {
                val manga = JSONObject().apply {
                    // Vérifier si chaque index de colonne est valide
                    if (titleIndex >= 0) put("title", cursor.getString(titleIndex))
                    if (volumeIntegralIndex >= 0) put("volumeIntegral", cursor.getInt(volumeIntegralIndex))
                    if (volumesOwnedIndex >= 0) put("volumesOwned", cursor.getInt(volumesOwnedIndex))
                    if (volumesReadIndex >= 0) put("volumesRead", cursor.getInt(volumesReadIndex))
                    if (stateIndex >= 0) put("state", cursor.getString(stateIndex))
                    if (chapitreIndex >= 0) put("chapitre", cursor.getInt(chapitreIndex))
                    if (coverUrlIndex >= 0) put("coverUrl", cursor.getString(coverUrlIndex))
                }
                mangasList.put(manga) // Ajouter chaque manga au JSONArray
            } while (cursor.moveToNext())
        }

        cursor.close()

        // Log les données avant d'écrire dans le fichier
        Log.d("MangaDatabaseHelper", "Mangas à sauvegarder: $mangasList")

        // Sauvegarder le JSON dans un fichier
        try {
            // Ecriture du fichier interne
            val fileOutputStreamInternal = appContext.openFileOutput("manga.json", Context.MODE_PRIVATE)
            val fileContent = mangasList.toString()
            fileOutputStreamInternal.write(fileContent.toByteArray())
            fileOutputStreamInternal.close()

            // Vérification dans les logs
            val fileInputStreamInternal = appContext.openFileInput("manga.json")
            val inputStreamReader = InputStreamReader(fileInputStreamInternal)
            val content = inputStreamReader.readText()
            Log.d("MangaDatabaseHelper", "Fichier interne après écriture: $content")
            inputStreamReader.close()

        } catch (e: IOException) {
            Log.e("MangaDatabaseHelper", "Erreur lors de l'écriture du fichier JSON.", e)
        }
    }

    companion object {
        private const val DATABASE_NAME = "mangas.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_MANGAS = "mangas"
        const val COLUMN_TITLE = "title"
        const val COLUMN_VOLUME_INTEGRAL = "volumeIntegral"
        const val COLUMN_VOLUMES_OWNED = "volumesOwned"
        const val COLUMN_VOLUMES_READ = "volumesRead"
        const val COLUMN_STATE = "state"
        const val COLUMN_CHAPITRE = "chapitre"
        const val COLUMN_URL = "coverUrl"
    }
}
