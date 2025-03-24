package iutlens.android.mymangalist

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import iutlens.android.mymangalist.adapter.AnimeAdapter
import iutlens.android.mymangalist.database.AnimeDatabaseHelper
import iutlens.android.mymangalist.databinding.ActivityAnimeBinding
import iutlens.android.mymangalist.model.Anime
import coil.load

class AnimeActivity : AppCompatActivity() {

    private lateinit var animeAdapter: AnimeAdapter
    private var animeList: MutableList<Anime> = mutableListOf()
    private lateinit var dbHelper: AnimeDatabaseHelper
    private lateinit var addButton: Button

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityAnimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val buttonMangas = findViewById<Button>(R.id.buttonMangas)

        buttonMangas.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val buttonNotes = findViewById<Button>(R.id.buttonNotes)

        buttonNotes.setOnClickListener {
            val intent = Intent(this, NotesActivity::class.java)
            startActivity(intent)
        }

        val searchView = findViewById<SearchView>(R.id.searchViewAnimes)

        val searchMagIcon =
            searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchMagIcon.setColorFilter(
            ContextCompat.getColor(this, R.color.white),
            PorterDuff.Mode.SRC_IN
        )

        val searchText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(ContextCompat.getColor(this, R.color.white))
        searchText.setHintTextColor(ContextCompat.getColor(this, R.color.white))

        searchView.setOnClickListener {
            searchView.setIconified(false)
        }

        dbHelper = AnimeDatabaseHelper(this)
        addButton = findViewById(R.id.buttonAddAnimeSeries)

        addButton.setOnClickListener {
            showAddAnimeDialog()
        }

        setupStatusBar()

        animeList = getAnimesFromDB().toMutableList()

        animeAdapter = AnimeAdapter(animeList) { anime -> showEditAnimeDialog(anime) }
        binding.recyclerViewAnimes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAnimes.adapter = animeAdapter

        updateAnimeUI()

        setupAnimeSearchView()
    }

    private fun addAnimeToDatabase(
        title: String,
        state: String,
        coverUrl: String
    ) {
        if (title.isEmpty()) {
            Toast.makeText(this, "Le titre est obligatoire", Toast.LENGTH_SHORT).show()
            return
        }

        val result = dbHelper.addAnime(title, state, coverUrl)

        if (result != -1L) {
            Toast.makeText(this, "Anime ajouté avec succès", Toast.LENGTH_SHORT).show()

            animeList = getAnimesFromDB().toMutableList()
            animeAdapter.updateList(animeList)
            updateAnimeUI()

            dbHelper.updateJSONFile()

        } else {
            Toast.makeText(this, "Erreur lors de l'ajout de l'anime", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupStatusBar() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.searchBackgroundDark)
        val window: Window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.statusBarColorDark)
    }

    private fun setupAnimeSearchView() {
        val searchView = findViewById<SearchView>(R.id.searchViewAnimes)

        val searchMagIconId = searchView.context.resources.getIdentifier(
            "android:id/search_mag_icon", null, null
        )
        val searchMagIcon = searchView.findViewById<ImageView>(searchMagIconId)
        searchMagIcon?.setColorFilter(
            ContextCompat.getColor(this, R.color.white),
            android.graphics.PorterDuff.Mode.SRC_IN
        )

        val searchTextId = searchView.context.resources.getIdentifier(
            "android:id/search_src_text", null, null
        )
        val searchEditText = searchView.findViewById<EditText>(searchTextId)
        searchEditText?.setTextColor(ContextCompat.getColor(this, R.color.white))
        searchEditText?.setHintTextColor(ContextCompat.getColor(this, R.color.white))

        val searchCloseButtonId = searchView.context.resources.getIdentifier(
            "android:id/search_close_btn", null, null
        )
        val searchCloseButton = searchView.findViewById<ImageView>(searchCloseButtonId)
        searchCloseButton?.setColorFilter(ContextCompat.getColor(this, R.color.white))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchMagIcon?.setColorFilter(
                    ContextCompat.getColor(
                        this@AnimeActivity,
                        R.color.white
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
                searchEditText?.setTextColor(
                    ContextCompat.getColor(
                        this@AnimeActivity,
                        R.color.white
                    )
                )
                searchEditText?.setHintTextColor(
                    ContextCompat.getColor(
                        this@AnimeActivity,
                        R.color.white
                    )
                )

                filterAnimes(newText)
                return true
            }
        })

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                searchMagIcon?.setColorFilter(
                    ContextCompat.getColor(
                        this@AnimeActivity,
                        R.color.white
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
                searchEditText?.setTextColor(
                    ContextCompat.getColor(
                        this@AnimeActivity,
                        R.color.white
                    )
                )
                searchEditText?.setHintTextColor(
                    ContextCompat.getColor(
                        this@AnimeActivity,
                        R.color.white
                    )
                )
            }
        }
    }

    private fun updateAnimeUI() {
        val totalAnimes = dbHelper.getTotalAnimes()

        val animeSeriesCounter = findViewById<TextView>(R.id.seriesAnimesCounter)

        animeSeriesCounter.text = "Séries: $totalAnimes"
    }

    private fun getAnimesFromDB(): List<Anime> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM animes ORDER BY title ASC", null)
        val animes = mutableListOf<Anime>()

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val state = cursor.getString(cursor.getColumnIndexOrThrow("state"))
            val coverUrl = cursor.getString(cursor.getColumnIndexOrThrow("coverUrl"))

            animes.add(Anime(id, title, state, emptyList(), coverUrl))
        }
        cursor.close()
        db.close()

        return animes
    }

    private fun filterAnimes(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            animeList
        } else {
            animeList.filter { it.title.contains(query, ignoreCase = true) }
        }

        animeAdapter.updateList(filteredList)
    }

    private fun showAddAnimeDialog() {
        val dialog = Dialog(this, R.style.MyAlertDialogTheme)

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_anime, null)
        dialog.setContentView(dialogView)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val spinnerState = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerState)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
        val editTextCoverUrl = dialogView.findViewById<EditText>(R.id.editTextCoverUrl)
        val buttonAdd = dialogView . findViewById < Button >(R.id.buttonAdd)
        val stateAdapter = android.widget.ArrayAdapter.createFromResource(
            this,
            R.array.state_options,
            R.layout.item_spinner
        )
        stateAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spinnerState.adapter = stateAdapter

        buttonAdd.setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val state = spinnerState.selectedItem.toString()
            val coverUrl = editTextCoverUrl.text.toString().trim()

            addAnimeToDatabase(title, state, coverUrl)
            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun showEditAnimeDialog(anime: Anime) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_anime, null)

        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val spinnerState = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerState)
        val editTextCoverUrl = dialogView.findViewById<EditText>(R.id.editTextCoverUrl)
        val coverImageView = dialogView.findViewById<ImageView>(R.id.coverUrl)
        val buttonDelete = dialogView.findViewById<Button>(R.id.buttonDelete)

        editTextTitle.setText(anime.title)
        editTextCoverUrl.setText(anime.coverUrl)

        coverImageView.load(anime.coverUrl) {
            placeholder(R.drawable.ic_launcher_foreground)
            error(R.drawable.ic_launcher_foreground)
        }

        val stateAdapter = android.widget.ArrayAdapter.createFromResource(
            this,
            R.array.state_options,
            R.layout.item_spinner
        )
        stateAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spinnerState.adapter = stateAdapter
        spinnerState.setSelection(stateAdapter.getPosition(anime.state))

        val alertDialog = AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
            .setView(dialogView)
            .setPositiveButton("ENREGISTRER") { _, _ ->
                val newTitle = editTextTitle.text.toString()
                val newState = spinnerState.selectedItem.toString()
                val newCoverUrl = editTextCoverUrl.text.toString().ifEmpty { anime.coverUrl }

                dbHelper.updateAnime(anime.id, newTitle, newState, newCoverUrl)

                val updatedAnime = anime.copy(
                    title = newTitle,
                    state = newState,
                    coverUrl = newCoverUrl
                )

                val index = animeList.indexOf(anime)
                if (index != -1) {
                    animeList[index] = updatedAnime
                }

                animeAdapter.notifyItemChanged(index)
                updateAnimeUI()

                coverImageView.load(newCoverUrl) {
                    placeholder(R.drawable.ic_launcher_foreground)
                    error(R.drawable.ic_launcher_foreground)
                }
            }
            .setNegativeButton("ANNULER", null)
            .create()

        alertDialog.show()

        buttonDelete.setOnClickListener {
            AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
                .setTitle("Supprimer l'anime")
                .setMessage("Voulez-vous vraiment supprimer ${anime.title} ?")
                .setPositiveButton("OUI") { _, _ ->
                    dbHelper.deleteAnime(anime.id)
                    animeList.remove(anime)
                    animeAdapter.notifyDataSetChanged()
                    updateAnimeUI()
                    alertDialog.dismiss()
                }
                .setNegativeButton("NON", null)
                .show()
        }
    }
}