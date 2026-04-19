package iutlens.android.mymangalist.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import iutlens.android.mymangalist.R
import iutlens.android.mymangalist.model.Serie

class MangaTitleAdapter(
    private var series: List<Serie>,
    private val onItemClick: (Serie) -> Unit
) : RecyclerView.Adapter<MangaTitleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.mangaTitleTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manga_title, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val serie = series[position]
        holder.titleTextView.text = serie.title
        holder.itemView.setOnClickListener { onItemClick(serie) }
    }

    override fun getItemCount() = series.size

    fun updateList(newSeries: List<Serie>) {
        series = newSeries
        notifyDataSetChanged()
    }
}
