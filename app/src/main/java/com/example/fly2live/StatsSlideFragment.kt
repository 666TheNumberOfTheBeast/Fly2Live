package com.example.fly2live

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.example.fly2live.player.Player
import com.example.fly2live.players_adapters.PlayersScoresAdapter
import com.example.fly2live.players_adapters.PlayersWinsAdapter
import org.json.JSONObject

class StatsSlideFragment() : Fragment() {
    private var position = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Retrieve additional information
        position = arguments!!.getInt("position")

        // Inflate the layout for this fragment
        when (position) {
            0 -> return inflater.inflate(R.layout.fragment_stats_slide_win, container, false)
            1 -> return inflater.inflate(R.layout.fragment_stats_slide_score, container, false)
        }

        return inflater.inflate(R.layout.fragment_stats_slide_win, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val names = arrayOf("Anna Paola", "Andrea", "Barbara", "Frank", "Ivan", "Maria", "Vittorio")
        val players = ArrayList<Player>()

        for (i in 1..20) {
            val player = JSONObject()
            player.put("name", names.random())
            player.put("W", Math.random() * 310)
            player.put("L", Math.random() * 310)
            player.put("best_score", Math.random() * 3100)

            val name      = player.getString("name")
            val wins      = player.getInt("W")
            val loses     = player.getInt("L")
            val bestScore = player.getLong("best_score")

            players.add(Player(name, wins, loses, bestScore))
        }

        lateinit var playersAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
        when (position) {
            0 -> {
                players.sortByDescending { player -> player.getWinPercentage() }
                @Suppress("UNCHECKED_CAST")
                playersAdapter = PlayersWinsAdapter(players) as RecyclerView.Adapter<RecyclerView.ViewHolder>
            }
            1 -> {
                players.sortByDescending { player -> player.getBestScore() }
                @Suppress("UNCHECKED_CAST")
                playersAdapter = PlayersScoresAdapter(players) as RecyclerView.Adapter<RecyclerView.ViewHolder>
            }
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = playersAdapter

        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            DividerItemDecoration.VERTICAL
        )
        recyclerView.addItemDecoration(dividerItemDecoration)
    }

}