package iutlens.android.mymangalist

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import iutlens.android.mymangalist.databinding.ActivityNotesBinding
import iutlens.android.mymangalist.model.Serie
import java.io.InputStreamReader

class NotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotesBinding
    private lateinit var noteEditText: EditText
    private lateinit var sharedPreferences: SharedPreferences

    private val IMPORT_JSON_REQUEST_CODE = 456
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteEditText = binding.noteEditText
        sharedPreferences = getSharedPreferences("MyNotes", MODE_PRIVATE)

        loadNotes()

        binding.buttonExportJson.setOnClickListener {
            exportLibrary()
        }

        binding.buttonHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.buttonImportJson.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "application/json"
            startActivityForResult(intent, IMPORT_JSON_REQUEST_CODE)
        }
    }

    override fun onPause() {
        super.onPause()
        saveNotes()
    }

    private fun loadNotes() {
        noteEditText.setText(sharedPreferences.getString("note", ""))
    }

    private fun saveNotes() {
        sharedPreferences.edit { putString("note", noteEditText.text.toString()) }
    }

    // Export : sauvegarde manga.json dans les Downloads
    private fun exportLibrary() {
        val library = MangaRepository.loadLibrary(this)
        val json = gson.toJson(library)
        saveJsonToDownloads(json, "manga.json")
        Toast.makeText(this, "Bibliothèque exportée ✅", Toast.LENGTH_SHORT).show()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMPORT_JSON_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                AlertDialog.Builder(this)
                    .setTitle("Importer JSON")
                    .setMessage("Fusionner avec la bibliothèque existante ou remplacer ?")
                    .setPositiveButton("Fusionner") { _, _ ->
                        val count = importLibraryFromUri(uri, merge = true)
                        Toast.makeText(this, "$count séries importées ✅", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Remplacer") { _, _ ->
                        val count = importLibraryFromUri(uri, merge = false)
                        Toast.makeText(this, "$count séries importées ✅", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        }
    }

    private fun importLibraryFromUri(uri: Uri, merge: Boolean): Int {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val jsonString = InputStreamReader(inputStream!!).readText()
            val type = object : TypeToken<MutableList<Serie>>() {}.type
            val imported: MutableList<Serie> = gson.fromJson(jsonString, type) ?: mutableListOf()

            if (merge) {
                val existing = MangaRepository.loadLibrary(this)
                imported.forEach { serie ->
                    if (existing.none { it.id == serie.id }) existing.add(serie)
                }
                MangaRepository.saveLibrary(this, existing)
            } else {
                MangaRepository.saveLibrary(this, imported)
            }

            imported.size
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erreur lors de l'import ❌", Toast.LENGTH_SHORT).show()
            0
        }
    }

    // Sauvegarde un JSON dans les Downloads (compatible Android 10+)
    private fun saveJsonToDownloads(json: String, fileName: String) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = contentResolver
                var uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

                if (uri == null) {
                    val cursor = resolver.query(
                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, null,
                        "${android.provider.MediaStore.Downloads.DISPLAY_NAME} = ?",
                        arrayOf(fileName), null
                    )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val id = it.getLong(it.getColumnIndexOrThrow(android.provider.MediaStore.Downloads._ID))
                            uri = Uri.withAppendedPath(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                        }
                    }
                }

                uri?.let {
                    resolver.openOutputStream(it)?.use { stream -> stream.write(json.toByteArray()) }
                }
            } else {
                val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                java.io.File(dir, fileName).writeText(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}