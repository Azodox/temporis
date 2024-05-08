package fr.azodox.temporis.leaderboard

import fr.azodox.temporis.tables.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.TextDisplay
import org.bukkit.plugin.Plugin
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.DecimalFormat
import java.util.*

data class Leaderboard(
    val plugin: Plugin,
    val displayName: Component,
    val topLocation: Location,
    val limit: Int,
    val entriesGap: Double
) {

    var entries = mutableListOf<UUID>()
    var topAnimationCooldown = mutableMapOf<UUID, Long>()

    private lateinit var topText: TextDisplay
    private val displayedEntries = mutableListOf<TextDisplay>()
    private val decimalFormat = DecimalFormat("#,###")
    private var allPlayers = Player

    init {
        spawn()
    }

    private fun spawn() {
        val world = topLocation.world
        topText = world.spawn(topLocation, TextDisplay::class.java)
        topText.text(displayName)

        for (i in 1..limit) {
            val entryLocation = topLocation.clone().add(0.0, -i * entriesGap, 0.0)
            val entryText = world.spawn(entryLocation, TextDisplay::class.java)
            entryText.text(
                Component.text("#$i").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
                    .append(Component.text(" - ").color(NamedTextColor.GRAY))
            )
            displayedEntries.add(entryText)
        }
    }

    fun updateEntries() {
        if (topText.isDead || displayedEntries.any { it.isDead })
            reload()

        transaction {
            val players = allPlayers.all().filter { it.depositedEmeralds != 0L }.sortedByDescending { it.depositedEmeralds }
            
            for ((index, player) in players.withIndex()) {
                entries.add(player.uuid)
                if (index >= limit)
                    break

                val entry = displayedEntries[index]
                entry.text(
                    Component.text("#${index + 1}").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
                        .append(Component.space())
                        .append(
                            Component.text(Bukkit.getOfflinePlayer(player.uuid).name ?: "")
                                .color(if (index == 0) NamedTextColor.YELLOW else NamedTextColor.WHITE)
                        )
                        .append(Component.text(" - ").color(NamedTextColor.GRAY))
                        .append(
                            Component.text(decimalFormat.format(player.depositedEmeralds).replace(",", " "))
                                .color(NamedTextColor.GREEN)
                        )
                )
            }
        }
    }

    fun getPlayerRank(uuid: UUID): Int {
        val rank = entries.indexOf(uuid)
        if (rank == -1)
            return -1
        return rank + 1
    }

    fun getOpponent(uuid: UUID): String {
        val rank = getPlayerRank(uuid)
        if (rank == -1)
            return "Non classé"
        return Bukkit.getOfflinePlayer(entries[rank - 2]).name ?: "Inconnu"
    }

    fun reload() {
        delete()
        spawn()
    }

    fun delete() {
        topText.remove()
        displayedEntries.forEach { it.remove() }
        displayedEntries.clear()
        entries.clear()
    }
}
