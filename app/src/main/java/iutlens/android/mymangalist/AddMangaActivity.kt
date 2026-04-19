package iutlens.android.mymangalist

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import iutlens.android.mymangalist.adapter.MangaTitleAdapter
import iutlens.android.mymangalist.databinding.ActivityAddMangaBinding
import iutlens.android.mymangalist.model.Serie

class AddMangaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMangaBinding

    private var allSeries: List<Serie> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddMangaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Catalogue trié alphabétique
        allSeries = MangaRepository.loadCatalogue(this)
            .sortedBy { it.title.lowercase() }

        setupRecycler()
        setupSearch()
    }

    private fun setupRecycler() {
        val adapter = MangaTitleAdapter(allSeries) { serie ->
            MangaRepository.addToLibrary(this, serie)

            val intent = Intent(this, MangaVolumesActivity::class.java)
            intent.putExtra("SERIE_ID", serie.id)
            startActivity(intent)
        }

        binding.recyclerViewMangas.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMangas.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {
                // Le filtrage préserve le tri alphabétique déjà appliqué sur allSeries
                val filtered = allSeries.filter {
                    it.title.contains(newText ?: "", ignoreCase = true)
                }
                (binding.recyclerViewMangas.adapter as MangaTitleAdapter).updateList(filtered)
                return true
            }
        })
    }
}