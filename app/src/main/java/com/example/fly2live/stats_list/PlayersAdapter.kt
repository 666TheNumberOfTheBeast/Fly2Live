package com.example.fly2live.stats_list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fly2live.R
import com.example.fly2live.Player


class PlayersAdapter(players: ArrayList<Player>) :
    ListAdapter<Player, PlayersAdapter.PlayerViewHolder>(PlayerDiffCallback) {
    private val data = players

    class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerPositionView       = itemView.findViewById<TextView>(R.id.player_position)
        private val playerNameView           = itemView.findViewById<TextView>(R.id.player_name)
        private val playerWinsView           = itemView.findViewById<TextView>(R.id.W)
        private val playerLosesView          = itemView.findViewById<TextView>(R.id.L)
        private val playerWinPercentageView  = itemView.findViewById<TextView>(R.id.win_percentage)

        private val playerPositionCell      = itemView.findViewById<LinearLayout>(R.id.player_position_cell)
        private val playerNameCell          = itemView.findViewById<LinearLayout>(R.id.player_name_cell)
        private val playerWinsCell          = itemView.findViewById<LinearLayout>(R.id.W_cell)
        private val playerLosesCell         = itemView.findViewById<LinearLayout>(R.id.L_cell)
        private val playerWinPercentageCell = itemView.findViewById<LinearLayout>(R.id.win_percentage_cell)

        private val context = itemView.context

        fun bind(player: Player) {
            playerPositionView.text       = player.getPosition().toString()
            playerNameView.text           = player.getName()
            playerWinsView.text           = player.getWins().toString()
            playerLosesView.text          = player.getLoses().toString()
            playerWinPercentageView.text  = player.getWinPercentage()

            // Interleave color of each row
            if (player.getPosition() % 2 == 0)
                setBackground(ContextCompat.getColor(context, R.color.blue_bg))
            else
                setBackground(ContextCompat.getColor(context, R.color.red_bg))//*/
        }

        private fun setBackground(colorID: Int) {
            playerPositionCell.setBackgroundColor(colorID)
            playerNameCell.setBackgroundColor(colorID)
            playerWinsCell.setBackgroundColor(colorID)
            playerLosesCell.setBackgroundColor(colorID)
            playerWinPercentageCell.setBackgroundColor(colorID)
        }

    }

    // Creates and inflates view and return PlayerViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.stats_item, parent, false)
        return PlayerViewHolder(view)
    }

    // Gets current player and uses it to bind view
    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = data[position]
        player.setPosition(position+1)
        holder.bind(player)
    }

    // Returns number of items
    override fun getItemCount(): Int {
        return data.size
    }
}

object PlayerDiffCallback : DiffUtil.ItemCallback<Player>() {
    override fun areItemsTheSame(oldItem: Player, newItem: Player): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean {
        return oldItem.getName() == newItem.getName()
    }
}