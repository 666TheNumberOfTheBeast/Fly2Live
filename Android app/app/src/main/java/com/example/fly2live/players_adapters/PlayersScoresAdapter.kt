package com.example.fly2live.players_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fly2live.R
import com.example.fly2live.player.Player


class PlayersScoresAdapter(players: ArrayList<Player>) :
    ListAdapter<Player, PlayersScoresAdapter.PlayerScoresViewHolder>(PlayerDiffCallback) {
    private val data = players

    class PlayerScoresViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerPositionView       = itemView.findViewById<TextView>(R.id.player_position)
        private val playerNameView           = itemView.findViewById<TextView>(R.id.player_name)
        private val playerValueView           = itemView.findViewById<TextView>(R.id.value)

        private val playerPositionCell      = itemView.findViewById<LinearLayout>(R.id.player_position_cell)
        private val playerNameCell          = itemView.findViewById<LinearLayout>(R.id.player_name_cell)
        private val playerValueCell          = itemView.findViewById<LinearLayout>(R.id.value_cell)

        private val context = itemView.context

        fun bind(player: Player) {
            playerPositionView.text       = player.getPosition().toString()
            playerNameView.text           = player.getName()
            playerValueView.text          = player.getBestScore().toString()

            // Interleave color of each row
            if (player.getPosition() % 2 == 0)
                setBackground(ContextCompat.getColor(context, R.color.green_bg))
            else
                setBackground(ContextCompat.getColor(context, R.color.orange_bg))//*/
        }

        private fun setBackground(colorID: Int) {
            playerPositionCell.setBackgroundColor(colorID)
            playerNameCell.setBackgroundColor(colorID)
            playerValueCell.setBackgroundColor(colorID)
        }

    }

    // Creates and inflates view and return PlayerViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerScoresViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.stats_players_scores_item, parent, false)
        return PlayerScoresViewHolder(view)
    }

    // Gets current player and uses it to bind view
    override fun onBindViewHolder(holder: PlayerScoresViewHolder, position: Int) {
        val player = data[position]
        player.setPosition(position+1)
        holder.bind(player)
    }

    // Returns number of items
    override fun getItemCount(): Int {
        return data.size
    }
}