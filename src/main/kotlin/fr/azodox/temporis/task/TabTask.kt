package fr.azodox.temporis.task

import fr.azodox.temporis.LEADERBOARD
import fr.azodox.temporis.MESSAGES
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import kotlin.jvm.optionals.getOrNull

object TabTask : Runnable {

    override fun run() {
        Bukkit.getServer().forEachAudience { audience ->
            if (audience[Identity.UUID].isEmpty)
                return@forEachAudience

            val uuid = audience[Identity.UUID].getOrNull() ?: return@forEachAudience
            val rank = LEADERBOARD.getPlayerRank(uuid)
            val footer = Component.text()
            footer.append(
                Component.newline(),
                MESSAGES.get("tablist.your-rank").replaceText {
                    it.matchLiteral("%rank%")
                        .replacement(if (rank == -1) "§cNon classé" else "§e#$rank${if (rank == 1) "\uE001" else ""}")
                },
            )
            if (rank != -1) {
                footer.append(
                    Component.newline(),
                    if (rank == 1) MESSAGES.get("tablist.no-opponent") else MESSAGES.get("tablist.opponent")
                        .replaceText {
                            it.matchLiteral("%opponent%").replacement(LEADERBOARD.getOpponent(uuid))
                        }
                )
            }

            audience.sendPlayerListFooter(footer)
            audience.sendPlayerListHeader(
                Component.text().append(
                    MESSAGES.get("tablist.title"),
                    Component.newline(),
                )
            )
        }
    }
}