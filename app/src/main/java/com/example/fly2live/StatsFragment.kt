package com.example.fly2live

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.json.JSONObject
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fly2live.stats_list.HeaderAdapter
import com.example.fly2live.stats_list.PlayersAdapter
import androidx.recyclerview.widget.DividerItemDecoration

class StatsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stats, container, false)
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

            val name    = player.getString("name")
            val wins    = player.getInt("W")
            val loses   = player.getInt("L")

            players.add(Player(name, wins, loses))
        }

        players.sortByDescending { player -> player.getWinPercentage() }

        // Instantiates HeaderAdapter and PlayersAdapter.
        // Both adapters are added to concatAdapter which displays the contents sequentially
        val headerAdapter  = HeaderAdapter()
        val playersAdapter = PlayersAdapter(players)
        val concatAdapter  = ConcatAdapter(headerAdapter, playersAdapter)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = concatAdapter

        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            DividerItemDecoration.VERTICAL
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

    }
}

class Player(name: String, wins: Int, loses: Int) {
    private val name: String
    private val wins: Int
    private val loses: Int
    private val win_percentage: String
    private var position = 0

    init {
        this.name           = name
        this.wins           = wins
        this.loses          = loses
        this.win_percentage = String.format("%.3f", wins / (wins + loses).toFloat())
    }

    // Getters
    fun getName(): String {
        return name
    }

    fun getWins(): Int {
        return wins
    }

    fun getLoses(): Int {
        return loses
    }

    fun getWinPercentage(): String {
        return win_percentage
    }

    fun getPosition(): Int {
        return position
    }

    // Setters
    fun setPosition(position: Int) {
        this.position = position
    }

    override fun toString(): String {
        return name + " " + win_percentage
    }
}