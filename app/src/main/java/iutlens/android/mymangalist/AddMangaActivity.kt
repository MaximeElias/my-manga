package iutlens.android.mymangalist

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import iutlens.android.mymangalist.database.MangaDatabaseHelper
import iutlens.android.mymangalist.databinding.ActivityAddMangaBinding

class AddMangaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMangaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddMangaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        setupButtons()
    }

    private fun setupSpinner() {
        val stateAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.state_options,
            R.layout.item_spinner
        )
        stateAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerState.adapter = stateAdapter
    }

    private fun setupButtons() {
        binding.buttonAdd.setOnClickListener {
            addSeries()
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }
    }

    private fun addSeries() {
        val title = binding.editTextTitle.text.toString().trim()
        val volumeIntegral = binding.editTextVolumeIntegral.text.toString().toIntOrNull() ?: 0
        val volumeOwned = binding.editTextVolumeOwned.text.toString().toIntOrNull() ?: 0
        val volumesRead = binding.editTextVolumesRead.text.toString().toIntOrNull() ?: 0
        val chapitre = binding.editTextChapitre.text.toString().toIntOrNull() ?: 0
        val state = binding.spinnerState.selectedItem.toString()
        val coverUrl = binding.editTextCoverUrl.text.toString().trim()

        addSeriesToDatabase(title, volumeIntegral, volumeOwned, volumesRead, chapitre, state, coverUrl)
        finish()
    }

    private fun addSeriesToDatabase(title: String, volumeIntegral: Int, volumeOwned: Int, volumesRead: Int, chapitre: Int, state: String, coverUrl: String) {
        val dbHelper = MangaDatabaseHelper(this)
        dbHelper.addManga(title, volumeIntegral, volumeOwned, volumesRead, chapitre, state, coverUrl)
    }
}