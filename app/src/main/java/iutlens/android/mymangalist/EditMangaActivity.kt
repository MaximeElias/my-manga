package iutlens.android.mymangalist

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import iutlens.android.mymangalist.databinding.ActivityEditMangaBinding

class EditMangaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditMangaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditMangaBinding.inflate(layoutInflater)
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
        binding.buttonValidate.setOnClickListener {
            // Save the edited manga to the database
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }

        binding.buttonDelete.setOnClickListener {
            // Delete the manga from the database
        }
    }
}