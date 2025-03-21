package iutlens.android.mymangalist

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import iutlens.android.mymangalist.database.MangaDatabaseHelper
import iutlens.android.mymangalist.database.saveMangasToJSON

class NotesActivity : AppCompatActivity() {

    private lateinit var noteEditText: EditText
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dbHelper: MangaDatabaseHelper // Ajoutez une instance de MangaDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_notes)
        noteEditText = findViewById(R.id.noteEditText)
        sharedPreferences = getSharedPreferences("MyNotes", MODE_PRIVATE)
        dbHelper = MangaDatabaseHelper(this) // Initialisez dbHelper

        loadNotes()

        val buttonMangas = findViewById<Button>(R.id.buttonMangas)
        val buttonExportJson = findViewById<Button>(R.id.buttonExportJson)

        buttonMangas.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        buttonExportJson.setOnClickListener {
            save() // Appelez saveMangasToJSON()
        }
    }

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    override fun onPause() {
        super.onPause()
        saveNotes()
    }

    private fun loadNotes() {
        val savedNote = sharedPreferences.getString("note", "")
        noteEditText.setText(savedNote)
    }

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    private fun saveNotes() {
        val editor = sharedPreferences.edit()
        editor.putString("note", noteEditText.text.toString())
        editor.apply()
    }

    private fun save() {
        saveMangasToJSON(this)
        Toast.makeText(this, "Mangas exportés vers JSON", Toast.LENGTH_SHORT).show()
    }
}