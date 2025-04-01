package iutlens.android.mymangalist

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import iutlens.android.mymangalist.adapter.MangaAdapter
import iutlens.android.mymangalist.database.MangaDatabaseHelper
import iutlens.android.mymangalist.database.MangaDatabaseHelper.Companion.COLUMN_TITLE
import iutlens.android.mymangalist.database.importMangasFromJSON
import iutlens.android.mymangalist.databinding.ActivityMainBinding
import iutlens.android.mymangalist.model.Manga
import coil.load

class MainActivity : AppCompatActivity() {

    private lateinit var mangaAdapter: MangaAdapter
    private var mangaList: MutableList<Manga> = mutableListOf()
    private lateinit var dbHelper: MangaDatabaseHelper

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = MangaDatabaseHelper(this)

        mangaList = getMangasFromDB().toMutableList()
        mangaAdapter = MangaAdapter(mangaList) { manga -> showEditDialog(manga) }
        binding.recyclerViewMangas.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMangas.adapter = mangaAdapter

        setupSearchView()

        setupStatusBar()

        importMangasFromJSON(this)

        updateTotalUI()

        val searchView = findViewById<SearchView>(R.id.searchView)

        val searchMagIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchMagIcon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN)

        val searchText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(ContextCompat.getColor(this, R.color.white))
        searchText.setHintTextColor(ContextCompat.getColor(this, R.color.white))

        searchView.setOnClickListener {
            searchView.setIconified(false)
        }

        binding.buttonNotes.setOnClickListener {
            val intent = Intent(this, NotesActivity::class.java)
            startActivity(intent)
        }

        binding.buttonAddSeries.setOnClickListener {
            val intent = Intent(this, AddMangaActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupStatusBar() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_gray)
        val window: Window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_gray)
    }

    private fun setupSearchView() {
        val searchView = findViewById<SearchView>(R.id.searchView)

        val searchMagIconId = searchView.context.resources.getIdentifier(
            "android:id/search_mag_icon", null, null
        )
        val searchMagIcon = searchView.findViewById<ImageView>(searchMagIconId)
        searchMagIcon?.setColorFilter(ContextCompat.getColor(this, R.color.black), android.graphics.PorterDuff.Mode.SRC_IN) // Change to black

        val searchTextId = searchView.context.resources.getIdentifier(
            "android:id/search_src_text", null, null
        )
        val searchEditText = searchView.findViewById<EditText>(searchTextId)
        searchEditText?.setTextColor(ContextCompat.getColor(this, R.color.black))
        searchEditText?.setHintTextColor(ContextCompat.getColor(this, R.color.black))

        val searchCloseButtonId = searchView.context.resources.getIdentifier(
            "android:id/search_close_btn", null, null
        )
        val searchCloseButton = searchView.findViewById<ImageView>(searchCloseButtonId)
        searchCloseButton?.setColorFilter(ContextCompat.getColor(this, R.color.black))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchMagIcon?.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.black), android.graphics.PorterDuff.Mode.SRC_IN)

                searchEditText?.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
                searchEditText?.setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))

                filterMangas(newText)
                return true
            }
        })

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                searchMagIcon?.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.black), android.graphics.PorterDuff.Mode.SRC_IN)
                searchEditText?.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
                searchEditText?.setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
            }
        }
    }

    fun updateTotalUI() {
        val totalTomesPossedes = dbHelper.getTotalTomesPossedes()
        val totalTomesLus = dbHelper.getTotalTomesLus()
        val totalSeries = dbHelper.getTotalSeries()

        val tomesPossedesCounter = findViewById<TextView>(R.id.tomesOwnedCounter)
        val tomesLusCounter = findViewById<TextView>(R.id.tomesLusCounter)
        val seriesCounter = findViewById<TextView>(R.id.seriesCounter)

        tomesPossedesCounter.text = "Possédés: $totalTomesPossedes"
        tomesLusCounter.text = "Lus: $totalTomesLus"
        seriesCounter.text = "Séries: $totalSeries"
    }

    private fun getMangasFromDB(): List<Manga> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM mangas ORDER BY $COLUMN_TITLE ASC", null)
        val mangas = mutableListOf<Manga>()

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val volumeIntegral = cursor.getInt(cursor.getColumnIndexOrThrow("volumeIntegral"))
            val volumesOwned = cursor.getInt(cursor.getColumnIndexOrThrow("volumesOwned"))
            val volumesRead = cursor.getInt(cursor.getColumnIndexOrThrow("volumesRead"))
            val chapitre = cursor.getInt(cursor.getColumnIndexOrThrow("chapitre"))
            val state = cursor.getString(cursor.getColumnIndexOrThrow("state"))
            val coverUrl = cursor.getString(cursor.getColumnIndexOrThrow("coverUrl"))

            mangas.add(Manga(id, title, volumeIntegral, volumesOwned, volumesRead, chapitre, state, coverUrl))
        }
        cursor.close()
        db.close()

        return mangas
    }

    private fun filterMangas(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            mangaList
        } else {
            mangaList.filter { it.title.contains(query, ignoreCase = true) }
        }

        mangaAdapter.updateList(filteredList)
    }
}
