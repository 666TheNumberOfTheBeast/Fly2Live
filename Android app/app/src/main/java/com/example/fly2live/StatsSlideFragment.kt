package com.example.fly2live

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.example.fly2live.player.Player
import com.example.fly2live.players_adapters.PlayersScoresAdapter
import com.example.fly2live.players_adapters.PlayersWinsAdapter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.games.EventsClient
import com.google.android.gms.games.Games
import com.google.android.gms.games.LeaderboardsClient
import com.google.android.gms.games.leaderboard.LeaderboardVariant.COLLECTION_PUBLIC
import com.google.android.gms.games.leaderboard.LeaderboardVariant.TIME_SPAN_ALL_TIME
import org.json.JSONObject

class StatsSlideFragment() : Fragment() {
    private var position = -1

    private var account: GoogleSignInAccount? = null

    private lateinit var mEventsClient: EventsClient
    private lateinit var mLeaderboardClient: LeaderboardsClient
    private val maxResultsPerPage = 25


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access control
        account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account == null) {
            // Attempt to pop the controller's back stack back to a specific destination
            findNavController().popBackStack(R.id.mainFragment, false)
            return
        }

        // Get events and leaderboards clients
        mEventsClient      = Games.getEventsClient(context!!, account!!)
        mLeaderboardClient = Games.getLeaderboardsClient(context!!, account!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Retrieve additional information
        position = arguments!!.getInt("position")

        // Inflate the layout for this fragment
        return when (position) {
            0 -> inflater.inflate(R.layout.fragment_stats_slide_wins, container, false)
            1 -> inflater.inflate(R.layout.fragment_stats_slide_scores, container, false)
            2 -> inflater.inflate(R.layout.fragment_stats_slide_score, container, false)
            else -> null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Map of players identified by their IDs
        val map = HashMap<String, Player>()

        // Check if the user is at the win fragment
        if (position == 0) {
            // Temp sol to populate the event
            mEventsClient.increment(getString(R.string.event_game_won_multiplayer_id), 1)

            mEventsClient
                .load(false)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val events = task.result.get()!!

                        for (event in events) {
                            /*Log.d("stats", "loaded event " + event.name)
                            Log.d("stats", "event formatted value: " + event.formattedValue)
                            Log.d("stats", "event value: " + event.value)
                            Log.d("stats", "event player: " + event.player)
                            Log.d("stats", "event player name: " + event.player.name)
                            Log.d("stats", "event player display name: " + event.player.displayName)*/

                            val playerEvent = event.player
                            val playerId = playerEvent.playerId

                            // Get the player from the map if found otherwise create it
                            val player =
                                if (map.containsKey(playerId))
                                    map[playerId]
                                else
                                    Player(playerEvent.displayName)

                            when (event.name){
                                "Game won - multiplayer"  -> player!!.setWins(event.value)
                                "Game lost - multiplayer" -> player!!.setLoses(event.value)
                            }

                            map[playerId] = player!!
                        }

                        // Release resources
                        events.release()

                        populateRecyclerView(map)
                    }
                    else {
                        // Handle Error
                        val message = task.exception?.message
                        Log.d("stats", "error $message")
                    }
                }
        }
        // Check if the user is at the scores fragment
        else if (position == 1) {
            // Temp sol to populate the leaderboard
            mLeaderboardClient.submitScore(getString(R.string.leaderboard_best_score_multiplayer_id), 9254)

            mLeaderboardClient
                .loadPlayerCenteredScores(getString(R.string.leaderboard_best_score_multiplayer_id), TIME_SPAN_ALL_TIME, COLLECTION_PUBLIC, maxResultsPerPage)
                .addOnCompleteListener{ task ->
                    if (task.isSuccessful) {
                        val leaderboardScores = task.result.get()
                        val leaderboardScoresBuffer = leaderboardScores?.scores!!
                        Log.d("stats", "leaderboardScoresBuffer: $leaderboardScoresBuffer")

                        for (leaderboardScore in leaderboardScoresBuffer) {
                            val rank = leaderboardScore.displayRank
                            val score = leaderboardScore.displayScore
                            val playerName = leaderboardScore.scoreHolderDisplayName
                            Log.d("stats", "player: $playerName\trank: $rank\tscore: $score")
                        }

                        leaderboardScores.release()
                    }
                    else {
                        // Handle Error
                        val message = task.exception?.message
                        Log.d("stats", "error $message")
                    }

                }

            // Do things... (see commented)
        }
        // Check if the user is at the score single player fragment
        else if (position == 2) {
            // Temp sol to populate the leaderboard
            mLeaderboardClient.submitScore(getString(R.string.leaderboard_best_score_single_player_id), 13679)

            mLeaderboardClient
                .loadPlayerCenteredScores(getString(R.string.leaderboard_best_score_single_player_id), TIME_SPAN_ALL_TIME, COLLECTION_PUBLIC, maxResultsPerPage)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val leaderboardScores = task.result.get()
                        val leaderboardScoresBuffer = leaderboardScores?.scores!!
                        Log.d("stats", "leaderboardScoresBuffer: $leaderboardScoresBuffer")

                        for (leaderboardScore in leaderboardScoresBuffer) {
                            val rank = leaderboardScore.displayRank
                            val score = leaderboardScore.displayScore
                            val playerName = leaderboardScore.scoreHolderDisplayName
                            Log.d("stats", "player: $playerName\trank: $rank\tscore: $score")
                        }

                        leaderboardScores.release()
                    } else {
                        // Handle Error
                        val message = task.exception?.message
                        Log.d("stats", "error $message")
                    }
                }

            /*mLeaderboardClient.getLeaderboardIntent(getString(R.string.leaderboard_best_score_single_player_id)).addOnSuccessListener {intent ->
                startActivityForResult(intent, 1)
            }*/

            val playerValueView = view.findViewById<TextView>(R.id.value)
            playerValueView.text = 5862.toString()
            return
        }



        /*val names = arrayOf("Anna Paola", "Andrea", "Barbara", "Frank", "Ivan", "Maria", "Vittorio")
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
        recyclerView.addItemDecoration(dividerItemDecoration)*/
    }

    private fun populateRecyclerView(map: Map<String, Player>) {

        // Create an Array of players from the map
        val players = ArrayList<Player>()

        for (player in map.values) {
            player.calculateWinPercentage()
            players.add(player)
        }

        // Populate adapter
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

        val recyclerView = view!!.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = playersAdapter

        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            DividerItemDecoration.VERTICAL
        )
        recyclerView.addItemDecoration(dividerItemDecoration)
    }

}