package iutlens.android.mymangalist.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import iutlens.android.mymangalist.MangaRepository
import iutlens.android.mymangalist.R
import iutlens.android.mymangalist.model.Chapter

class ChapterAdapter(
    private val chapters: List<Chapter>,
    private val serieId: Int,
    private val volumeNumber: Int,
    private val context: Context,
    private val onChapterChanged: () -> Unit = {}
) : RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chapter, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        holder.bind(chapters[position])
    }

    override fun getItemCount() = chapters.size

    inner class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val chapterCheckBox: CheckBox = itemView.findViewById(R.id.chapterCheckBox)

        fun bind(chapter: Chapter) {
            val text = if (chapter.name.isNotEmpty())
                "${chapter.number}. ${chapter.name}"
            else
                "Chapitre ${chapter.number}"

            chapterCheckBox.text = text

            chapterCheckBox.buttonTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(Color.RED, Color.GRAY)
            )

            // Lit l'état sauvegardé
            chapter.isRead = MangaRepository.getChapterRead(
                context, serieId, volumeNumber, chapter.number, chapter.isRead
            )

            chapterCheckBox.setOnCheckedChangeListener(null)
            chapterCheckBox.isChecked = chapter.isRead

            chapterCheckBox.setOnCheckedChangeListener { _, isChecked ->
                chapter.isRead = isChecked
                MangaRepository.saveChapterRead(context, serieId, volumeNumber, chapter.number, isChecked)
                onChapterChanged()
            }
        }
    }
}