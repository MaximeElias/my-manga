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
import androidx.core.content.edit
import iutlens.android.mymangalist.databinding.ActivityNotesBinding

class NotesActivity : AppCompatActivity() {

    private lateinit var noteEditText: EditText
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dbHelper: MangaDatabaseHelper
    private lateinit var binding: ActivityNotesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteEditText = findViewById(R.id.noteEditText)
        sharedPreferences = getSharedPreferences("MyNotes", MODE_PRIVATE)
        dbHelper = MangaDatabaseHelper(this)

        loadNotes()

        val buttonExportJson = findViewById<Button>(R.id.buttonExportJson)

        buttonExportJson.setOnClickListener {
            save()
        }

        binding.buttonHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java) // Assurez-vous que MainActivity est correct
            startActivity(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        saveNotes()
    }

    private fun loadNotes() {
        val savedNote = sharedPreferences.getString("note", "")
        noteEditText.setText(savedNote)
    }

    private fun saveNotes() {
        sharedPreferences.edit() {
            putString("note", noteEditText.text.toString())
        }
    }

    private fun save() {
        saveMangasToJSON(this)
        Toast.makeText(this, "Mangas exportés vers JSON", Toast.LENGTH_SHORT).show()
    }
}