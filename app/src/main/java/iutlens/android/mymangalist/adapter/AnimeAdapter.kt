package iutlens.android.mymangalist.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import iutlens.android.mymangalist.R
import iutlens.android.mymangalist.databinding.ItemAnimeBinding
import iutlens.android.mymangalist.model.Anime
import coil.load
import coil.request.CachePolicy

class AnimeAdapter(
    private var animes: List<Anime>,
    private val onEditClick: (Anime) -> Unit
) : RecyclerView.Adapter<AnimeAdapter.AnimeViewHolder>() {

    fun updateList(newList: List<Anime>) {
        animes = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeViewHolder {
        val binding = ItemAnimeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimeViewHolder, position: Int) {
        val anime = animes[position]
        Log.d("AnimeAdapter", "Binding anime: ${anime.title}, cover URL: ${anime.coverUrl}")
        holder.bind(anime)
    }

    override fun getItemCount(): Int = animes.size

    inner class AnimeViewHolder(private val binding: ItemAnimeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(anime: Anime) {
            binding.animeTitle.text = anime.title
            binding.animeState.text = "Statut: ${anime.state}"

            // Gestion des saisons
            binding.seasonsContainer.removeAllViews()

            anime.seasons.forEach { season ->
                val seasonLayout = LayoutInflater.from(binding.root.context).inflate(R.layout.item_season, binding.seasonsContainer, false) as LinearLayout
                val seasonTitle = seasonLayout.findViewById<TextView>(R.id.seasonTitle)
                val seasonProgressBar = seasonLayout.findViewById<ProgressBar>(R.id.seasonProgressBar)

                seasonTitle.text = "Saison ${season.seasonNumber} - ${season.episodesWatched}/${season.totalEpisodes} épisodes vus"

                val progressPercentage = if (season.totalEpisodes > 0) (season.episodesWatched.toFloat() / season.totalEpisodes * 100).toInt() else 0
                seasonProgressBar.progress = progressPercentage

                binding.seasonsContainer.addView(seasonLayout)
            }

            binding.editButton.setOnClickListener {
                onEditClick(anime)
            }

            // Gestion de l'image de couverture
            if (anime.coverUrl.startsWith("http")) {
                binding.animeCover.load(anime.coverUrl) {
                    placeholder(R.drawable.default_cover)
                    error(R.drawable.default_cover)
                    crossfade(true)
                    networkCachePolicy(CachePolicy.ENABLED)
                    listener(
                        onError = { _, _ ->
                            Log.e("AnimeAdapter", "Error loading image from URL: ${anime.coverUrl}")
                            binding.animeCover.setImageResource(R.drawable.default_cover)
                        }
                    )
                }
            } else {
                val resourceId = binding.root.context.resources.getIdentifier(anime.coverUrl.replace(".png", ""), "drawable", binding.root.context.packageName)
                if (resourceId != 0) {
                    binding.animeCover.setImageResource(resourceId)
                } else {
                    binding.animeCover.setImageResource(R.drawable.default_cover)
                }
            }
        }
    }
}