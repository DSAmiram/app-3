package com.example.pdfsearch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ResultsAdapter(
    private val results: List<SearchResult>,
    private val onClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<ResultsAdapter.ResultViewHolder>() {

    class ResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pageNumber: TextView = view.findViewById(R.id.txtPageNumber)
        val snippet: TextView = view.findViewById(R.id.txtSnippet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val item = results[position]
        holder.pageNumber.text = "صفحه ${item.pageIndex + 1}"
        holder.snippet.text = item.snippet
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = results.size
}
