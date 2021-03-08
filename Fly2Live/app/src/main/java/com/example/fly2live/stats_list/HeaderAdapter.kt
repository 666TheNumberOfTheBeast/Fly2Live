package com.example.fly2live.stats_list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fly2live.R

// A list always displaying one element: the header of the list of players stats
class HeaderAdapter: RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder>() {

    // ViewHolder for displaying header
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view){
        fun bind() {}
    }

    // Inflates view and returns HeaderViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.header_item, parent, false)
        return HeaderViewHolder(view)
    }

    // Bind the view of the header
    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind()
    }

    // Returns number of items
    override fun getItemCount(): Int {
        return 1
    }
}