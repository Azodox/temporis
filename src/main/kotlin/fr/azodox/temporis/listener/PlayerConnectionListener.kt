package fr.azodox.temporis.listener

import fr.azodox.temporis.BOARDS
import fr.azodox.temporis.Temporis
import fr.azodox.temporis.tables.Player
import fr.azodox.temporis.tables.Players
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.jetbrains.exposed.sql.transactions.transaction

class PlayerQuitListener : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        BOARDS.remove(player.uniqueId)?.delete()
    }

}

class PlayerJoinListener(private val temporis: Temporis) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        transaction {
            Player.find { Players.uuid eq player.uniqueId }.firstOrNull() ?: Player.new {
                uuid = player.uniqueId
            }
        }

        temporis.initBoard(player)
    }
}
