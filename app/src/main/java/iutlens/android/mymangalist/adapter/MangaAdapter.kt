package iutlens.android.mymangalist.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import iutlens.android.mymangalist.R
import iutlens.android.mymangalist.databinding.ItemMangaBinding
import iutlens.android.mymangalist.model.Serie

class MangaAdapter(
    private var mangas: List<Serie>,
    private val onItemClick: (Serie) -> Unit
) : RecyclerView.Adapter<MangaAdapter.MangaViewHolder>() {

    fun updateList(newList: List<Serie>) {
        mangas = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaViewHolder {
        val binding = ItemMangaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MangaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MangaViewHolder, position: Int) {
        holder.bind(mangas[position])
    }

    override fun getItemCount() = mangas.size

    inner class MangaViewHolder(private val binding: ItemMangaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(manga: Serie) {
            val context = binding.root.context
            val total   = manga.volumeIntegral

            binding.mangaTitle.text = manga.title

            // status vient directement du JSON : "En cours", "Terminé", etc.
            val status = manga.status.orEmpty()
            binding.mangaState.text = status.ifEmpty { "—" }

            val statusColor = when (status.lowercase()) {
                "en cours"              -> ContextCompat.getColor(context, R.color.orange)
                "terminé", "terminée"   -> ContextCompat.getColor(context, R.color.light_green)
                "abandonné", "abandonnée" -> ContextCompat.getColor(context, R.color.dark_red)
                else                    -> ContextCompat.getColor(context, R.color.black)
            }
            binding.mangaState.setTextColor(statusColor)

            // Tomes possédés
            binding.mangaVolumesOwned.text = "Tomes possédés: ${manga.volumesOwned}/$total"
            val progressOwned = if (total > 0) (manga.volumesOwned.toFloat() / total * 100).toInt() else 0
            binding.progressBar.progress = progressOwned

            // Tomes lus
            binding.mangaVolumesRead.text = "Tomes lus: ${manga.volumesRead}/$total"
            val progressRead = if (total > 0) (manga.volumesRead.toFloat() / total * 100).toInt() else 0
            binding.progressBar2.progress = progressRead

            // Dernier chapitre lu
            val last = manga.lastChapterRead
            binding.chapitre.text = if (last > 0) "Dernier chapitre lu: $last" else "Aucun chapitre lu"

            binding.root.setOnClickListener { onItemClick(manga) }

            // Couverture
            if (manga.coverUrl.startsWith("http")) {
                binding.mangaCover.load(manga.coverUrl) {
                    placeholder(R.drawable.default_cover)
                    error(R.drawable.default_cover)
                    crossfade(true)
                    networkCachePolicy(CachePolicy.ENABLED)
                }
            } else {
                val resourceId = context.resources.getIdentifier(
                    manga.coverUrl.replace(".png", ""),
                    "drawable",
                    context.packageName
                )
                binding.mangaCover.setImageResource(
                    if (resourceId != 0) resourceId else R.drawable.default_cover
                )
            }
        }
    }
}