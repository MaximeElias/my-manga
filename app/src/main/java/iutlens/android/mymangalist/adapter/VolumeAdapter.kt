package iutlens.android.mymangalist.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import iutlens.android.mymangalist.MangaRepository
import iutlens.android.mymangalist.R
import iutlens.android.mymangalist.model.Volume

class VolumeAdapter(
    private val volumes: MutableList<Volume>,
    private val serieId: Int,
    private val context: Context,
    private val onStatsChanged: () -> Unit = {}
) : RecyclerView.Adapter<VolumeAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_volume, parent, false)
        return VH(view)
    }

    override fun getItemCount() = volumes.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(volumes[position])
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val title    = itemView.findViewById<TextView>(R.id.volumeTitle)
        private val owned    = itemView.findViewById<CheckBox>(R.id.ownedCheckBox)
        private val read     = itemView.findViewById<CheckBox>(R.id.readCheckBox)
        private val recycler = itemView.findViewById<RecyclerView>(R.id.recyclerViewChapters)

        fun bind(volume: Volume) {
            title.text = "Tome ${volume.number}"

            val colorList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(Color.RED, Color.GRAY)
            )
            owned.buttonTintList = colorList
            read.buttonTintList  = colorList

            owned.setOnCheckedChangeListener(null)
            read.setOnCheckedChangeListener(null)

            owned.isChecked = volume.isOwned
            read.isChecked  = volume.isRead

            owned.setOnCheckedChangeListener { _, isChecked ->
                volume.isOwned = isChecked
                MangaRepository.saveVolumeOwned(context, serieId, volume.number, isChecked)
                onStatsChanged()
            }

            read.setOnCheckedChangeListener { _, isChecked ->
                volume.isRead = isChecked
                MangaRepository.saveVolumeRead(context, serieId, volume.number, isChecked)

                // Coche OU décoche tous les chapitres selon l'état du tome
                volume.chapters.forEach { chapter ->
                    chapter.isRead = isChecked
                    MangaRepository.saveChapterRead(
                        context, serieId, volume.number, chapter.number, isChecked
                    )
                }
                // Rafraîchit la liste des chapitres si elle est visible
                recycler.adapter?.notifyDataSetChanged()

                onStatsChanged()
            }

            // Crée l'adapter chapitres une seule fois par tome
            val chapterAdapter = ChapterAdapter(
                chapters         = volume.chapters,
                serieId          = serieId,
                volumeNumber     = volume.number,
                context          = context,
                onChapterChanged = {
                    // Auto-coche/décoche le tome selon l'état de tous les chapitres
                    val allRead = volume.chapters.isNotEmpty() && volume.chapters.all { it.isRead }
                    if (volume.isRead != allRead) {
                        volume.isRead = allRead
                        MangaRepository.saveVolumeRead(context, serieId, volume.number, allRead)
                        // Met à jour la checkbox sans retrigger le listener
                        read.setOnCheckedChangeListener(null)
                        read.isChecked = allRead
                        read.setOnCheckedChangeListener { _, isChecked ->
                            volume.isRead = isChecked
                            MangaRepository.saveVolumeRead(context, serieId, volume.number, isChecked)
                            volume.chapters.forEach { chapter ->
                                chapter.isRead = isChecked
                                MangaRepository.saveChapterRead(context, serieId, volume.number, chapter.number, isChecked)
                            }
                            recycler.adapter?.notifyDataSetChanged()
                            onStatsChanged()
                        }
                    }
                    onStatsChanged()
                }
            )

            recycler.layoutManager = LinearLayoutManager(itemView.context)
            recycler.adapter = chapterAdapter

            recycler.visibility = View.GONE
            itemView.setOnClickListener {
                recycler.visibility =
                    if (recycler.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
    }
}