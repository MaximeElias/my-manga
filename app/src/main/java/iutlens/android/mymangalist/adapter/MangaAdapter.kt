package iutlens.android.mymangalist.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import iutlens.android.mymangalist.R
import iutlens.android.mymangalist.databinding.ItemMangaBinding
import iutlens.android.mymangalist.model.Manga
import coil.load
import coil.request.CachePolicy

class MangaAdapter(
    private var mangas: List<Manga>,
    private val onEditClick: (Manga) -> Unit
) : RecyclerView.Adapter<MangaAdapter.MangaViewHolder>() {

    fun updateList(newList: List<Manga>) {
        mangas = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaViewHolder {
        val binding = ItemMangaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MangaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MangaViewHolder, position: Int) {
        val manga = mangas[position]
        Log.d("MangaAdapter", "Binding manga: ${manga.title}, cover URL: ${manga.coverUrl}")
        holder.bind(manga)
    }

    override fun getItemCount(): Int = mangas.size

    inner class MangaViewHolder(private val binding: ItemMangaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(manga: Manga) {
            binding.mangaTitle.text = manga.title
            binding.mangaState.text = manga.state
            binding.mangaVolumesOwned.text = "Tomes possédés: ${manga.volumesOwned}/${manga.volumeIntegral}"

            val totalVolumes = manga.volumeIntegral
            val volumesRead = manga.volumesRead
            val volumesOwned = manga.volumesOwned

            val progressPercentage = if (totalVolumes > 0) (volumesOwned.toFloat() / totalVolumes * 100).toInt() else 0
            binding.progressBar.progress = progressPercentage
            binding.mangaVolumesRead.text = "Tomes lus: ${manga.volumesRead}/${manga.volumeIntegral}"

            val progressPercentage2 = if (totalVolumes > 0) (volumesRead.toFloat() / totalVolumes * 100).toInt() else 0
            binding.progressBar2.progress = progressPercentage2

            val context = binding.root.context
            val textColor = when (manga.state) {
                "Fini" -> ContextCompat.getColor(context, R.color.green)
                "Abandonné" -> ContextCompat.getColor(context, R.color.red)
                else -> ContextCompat.getColor(context, R.color.orange)
            }

            binding.chapitre.text = "Chapitre lu: ${manga.chapitre}"
            binding.mangaState.setTextColor(textColor)

            binding.editButton.setOnClickListener {
                onEditClick(manga)
            }

            // Gérer les images locales ou en ligne
            if (manga.coverUrl.startsWith("http")) {
                binding.mangaCover.load(manga.coverUrl) {
                    placeholder(R.drawable.default_cover)
                    error(R.drawable.default_cover)
                    crossfade(true)
                    networkCachePolicy(CachePolicy.ENABLED)
                    listener(
                        onError = { _, _ ->
                            Log.e("MangaAdapter", "Error loading image from URL: ${manga.coverUrl}")
                            binding.mangaCover.setImageResource(R.drawable.default_cover)
                        }
                    )
                }
            } else {
                val resourceId = context.resources.getIdentifier(manga.coverUrl.replace(".png", ""), "drawable", context.packageName)
                if (resourceId != 0) {
                    binding.mangaCover.setImageResource(resourceId)
                } else {
                    binding.mangaCover.setImageResource(R.drawable.default_cover)
                }
            }
        }
    }
}
