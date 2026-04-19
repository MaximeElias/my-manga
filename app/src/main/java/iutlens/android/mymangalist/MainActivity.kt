package iutlens.android.mymangalist

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import iutlens.android.mymangalist.adapter.MangaAdapter
import iutlens.android.mymangalist.databinding.ActivityMainBinding
import iutlens.android.mymangalist.model.Serie

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mangaAdapter: MangaAdapter

    private var libraryList: MutableList<Serie> = mutableListOf()
    private var displayedList: List<Serie> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_gray)

        mangaAdapter = MangaAdapter(emptyList()) { serie ->
            val intent = Intent(this, MangaVolumesActivity::class.java)
            intent.putExtra("SERIE_ID", serie.id)
            startActivity(intent)
        }

        binding.recyclerViewMangas.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMangas.adapter = mangaAdapter

        setupSearchView()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        reloadLibrary()
    }

    private fun reloadLibrary() {
        MangaRepository.syncLibraryWithCatalogue(this)

        val loaded = MangaRepository.loadLibrary(this)
        MangaRepository.applyStates(this, loaded)

        libraryList.clear()
        libraryList.addAll(loaded)

        // Filtre ≥ 1 chapitre lu + tri alphabétique
        displayedList = libraryList
            .filter { it.chaptersRead > 0 }
            .sortedBy { it.title.lowercase() }

        mangaAdapter.updateList(displayedList)
        updateGlobalStats()
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val filtered = if (newText.isNullOrEmpty()) {
                    displayedList
                } else {
                    displayedList.filter { it.title.contains(newText, ignoreCase = true) }
                }
                mangaAdapter.updateList(filtered)
                return true
            }
        })
    }

    private fun updateGlobalStats() {
        binding.tomesOwnedCounter.text = "Tomes possédés: ${libraryList.sumOf { it.volumesOwned }}"
        binding.tomesLusCounter.text   = "Tomes lus: ${libraryList.sumOf { it.volumesRead }}"
        binding.seriesCounter.text     = "Séries: ${displayedList.size}"
    }

    private fun setupBottomNavigation() {
        findViewById<ImageButton>(R.id.buttonAddSeries).setOnClickListener {
            startActivity(Intent(this, AddMangaActivity::class.java))
        }
        findViewById<ImageButton>(R.id.buttonHome).setOnClickListener { }
        findViewById<ImageButton>(R.id.buttonNotes).setOnClickListener {
            startActivity(Intent(this, NotesActivity::class.java))
        }
    }
}