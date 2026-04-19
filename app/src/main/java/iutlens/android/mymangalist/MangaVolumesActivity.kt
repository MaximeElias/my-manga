package iutlens.android.mymangalist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import iutlens.android.mymangalist.adapter.VolumeAdapter
import iutlens.android.mymangalist.databinding.ActivityMangaVolumesBinding
import iutlens.android.mymangalist.model.Serie

class MangaVolumesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMangaVolumesBinding
    private lateinit var currentSerie: Serie

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMangaVolumesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serieId = intent.getIntExtra("SERIE_ID", -1)
        if (serieId == -1) { finish(); return }

        val catalogue = MangaRepository.loadCatalogue(this)
        val library   = MangaRepository.loadLibrary(this)

        val serie = catalogue.find { it.id == serieId }
            ?: library.find { it.id == serieId }
            ?: run { finish(); return }

        MangaRepository.applyStates(this, listOf(serie))
        currentSerie = serie

        bindHeader(serie)
        bindStats(serie)

        binding.recyclerViewVolumes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewVolumes.adapter = VolumeAdapter(
            volumes          = serie.volumes,
            serieId          = serie.id,
            context          = this,
            onStatsChanged   = { bindStats(serie) }
        )
    }

    private fun bindHeader(serie: Serie) {
        binding.titleTextView.text = serie.title
        binding.coverImageView.load(serie.coverUrl) {
            placeholder(R.drawable.default_cover)
            error(R.drawable.default_cover)
            crossfade(true)
        }

        val status = serie.status.orEmpty()
        val statusColor = when (status.lowercase()) {
            "en cours"              -> ContextCompat.getColor(this, R.color.orange)
            "terminé", "terminée"   -> ContextCompat.getColor(this, R.color.light_green)
            "abandonné", "abandonnée" -> ContextCompat.getColor(this, R.color.dark_red)
            else                    -> ContextCompat.getColor(this, R.color.black)
        }
        binding.statusTextView.text = status.ifEmpty { "—" }
        binding.statusTextView.setTextColor(statusColor)
    }

    private fun bindStats(serie: Serie) {
        val total = serie.volumeIntegral

        binding.volumesOwnedTextView.text =
            "📦 Tomes possédés : ${serie.volumesOwned} / $total"

        binding.volumesReadTextView.text =
            "📖 Tomes lus : ${serie.volumesRead} / $total"

        val progressRead = if (total > 0)
            (serie.volumesRead.toFloat() / total * 100).toInt() else 0
        binding.progressBarRead.progress = progressRead

        binding.chaptersReadTextView.text =
            "✅ Chapitres lus : ${serie.chaptersRead} / ${serie.totalChapters}"

        val last = serie.lastChapterRead
        binding.lastChapterReadTextView.text =
            if (last > 0) "🔖 Dernier chapitre lu : $last"
            else          "🔖 Aucun chapitre lu"
    }
}