package fr.azodox.temporis.events

import fr.azodox.temporis.leaderboard.Leaderboard
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.*

open class LeaderboardEvent(open val leaderboard: Leaderboard) : Event(){
    override fun getHandlers(): HandlerList {
        return HANDLERS_LIST
    }

    companion object {
        private val HANDLERS_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS_LIST
        }
    }
}

class LeaderboardEntriesUpdateEvent(leaderboard: Leaderboard, val oldEntries: List<UUID>) :
    LeaderboardEvent(leaderboard) {}

class LeaderboardPlayerRankUpdatedEvent(leaderboard: Leaderboard, val playerUUID: UUID, val oldRank: Int, val newRank: Int) :
    LeaderboardEvent(leaderboard) {}

class LeaderboardPlayerReachTopEvent(leaderboard: Leaderboard, val player: Player) :
    LeaderboardEvent(leaderboard) {}