package iutlens.android.mymangalist.database

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

fun importMangasFromJSON(context: Context) {
    val dbHelper = MangaDatabaseHelper(context)
    val db = dbHelper.writableDatabase

    // Vérifie si la table est vide avant d'importer
    val cursor = db.query(MangaDatabaseHelper.TABLE_MANGAS, null, null, null, null, null, null)
    if (cursor.count == 0) {
        val jsonFile = File(context.filesDir, "manga.json")
        if (jsonFile.exists()) {
            // Ouvre et lit le fichier JSON
            val jsonString = jsonFile.readText()
            val jsonArray = JSONArray(jsonString)

            // Insère les mangas dans la base de données
            for (i in 0 until jsonArray.length()) {
                val mangaObject = jsonArray.getJSONObject(i)
                val values = ContentValues().apply {
                    put(MangaDatabaseHelper.COLUMN_TITLE, mangaObject.getString("title"))
                    put(MangaDatabaseHelper.COLUMN_VOLUME_INTEGRAL, mangaObject.getInt("volumeIntegral"))
                    put(MangaDatabaseHelper.COLUMN_VOLUMES_OWNED, mangaObject.getInt("volumesOwned"))
                    put(MangaDatabaseHelper.COLUMN_VOLUMES_READ, mangaObject.getInt("volumesRead"))
                    put(MangaDatabaseHelper.COLUMN_STATE, mangaObject.getString("state"))
                    put(MangaDatabaseHelper.COLUMN_CHAPITRE, mangaObject.getInt("chapitre"))
                    put(MangaDatabaseHelper.COLUMN_URL, mangaObject.getString("coverUrl"))
                }
                db.insert(MangaDatabaseHelper.TABLE_MANGAS, null, values)
            }
        }
    }
    cursor.close()
    db.close()
}

fun saveMangasToJSON(context: Context) {
    val dbHelper = MangaDatabaseHelper(context)
    val db = dbHelper.readableDatabase
    val cursor = db.query(MangaDatabaseHelper.TABLE_MANGAS, null, null, null, null, null, "title ASC")

    val jsonArray = JSONArray()
    try {
        while (cursor.moveToNext()) {
            val manga = JSONObject()
            manga.put("title", cursor.getString(cursor.getColumnIndexOrThrow(MangaDatabaseHelper.COLUMN_TITLE)))
            manga.put("volumeIntegral", cursor.getInt(cursor.getColumnIndexOrThrow(MangaDatabaseHelper.COLUMN_VOLUME_INTEGRAL)))
            manga.put("volumesOwned", cursor.getInt(cursor.getColumnIndexOrThrow(MangaDatabaseHelper.COLUMN_VOLUMES_OWNED)))
            manga.put("volumesRead", cursor.getInt(cursor.getColumnIndexOrThrow(MangaDatabaseHelper.COLUMN_VOLUMES_READ)))
            manga.put("state", cursor.getString(cursor.getColumnIndexOrThrow(MangaDatabaseHelper.COLUMN_STATE)))
            manga.put("chapitre", cursor.getInt(cursor.getColumnIndexOrThrow(MangaDatabaseHelper.COLUMN_CHAPITRE)))
            manga.put("coverUrl", cursor.getString(cursor.getColumnIndexOrThrow(MangaDatabaseHelper.COLUMN_URL)))

            jsonArray.put(manga)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        cursor.close()
        db.close()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10 et supérieur
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "manga.json")
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        var uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri == null) {
            val queryCursor = resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, null,
                MediaStore.Downloads.DISPLAY_NAME + " = ?", arrayOf("manga.json"), null)

            queryCursor?.use { cursor_query ->
                if (cursor_query.moveToFirst()) {
                    val id = cursor_query.getLong(cursor_query.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                    uri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                }
            }
        }

        uri?.let {
            try {
                val outputStream: OutputStream? = resolver.openOutputStream(it)
                outputStream?.use { stream ->
                    stream.write(jsonArray.toString().toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } ?: run {
        }
    } else {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, "manga.json")

            val outputStream = FileOutputStream(file, false)
            outputStream.write(jsonArray.toString().toByteArray())
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}