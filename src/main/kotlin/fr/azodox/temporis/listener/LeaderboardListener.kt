package fr.azodox.temporis.listener

import fr.azodox.temporis.LEADERBOARD
import fr.azodox.temporis.MESSAGES
import fr.azodox.temporis.events.LeaderboardEntriesUpdateEvent
import fr.azodox.temporis.events.LeaderboardPlayerRankUpdatedEvent
import fr.azodox.temporis.events.LeaderboardPlayerReachTopEvent
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.time.Duration

class LeaderboardEntriesUpdateListener : Listener {

    @EventHandler
    fun onLeaderboardEntriesUpdate(event: LeaderboardEntriesUpdateEvent) {
        LEADERBOARD.entries.forEach { entry ->
            if (entry in event.oldEntries) {
                if (event.oldEntries.indexOf(entry) != LEADERBOARD.entries.indexOf(entry)) {
                    Bukkit.getPluginManager().callEvent(
                        LeaderboardPlayerRankUpdatedEvent(
                            event.leaderboard,
                            entry,
                            event.oldEntries.indexOf(entry) + 1,
                            LEADERBOARD.entries.indexOf(entry) + 1
                        )
                    )
                }
            }
        }
    }
}

class LeaderboardPlayerRankUpdatedListener : Listener {

    @EventHandler
    fun onLeaderboardPlayerRankUpdated(event: LeaderboardPlayerRankUpdatedEvent) {
        val player = Bukkit.getPlayer(event.playerUUID) ?: return

        if (event.oldRank != 1 && event.newRank == 1) {
            Bukkit.getPluginManager().callEvent(LeaderboardPlayerReachTopEvent(event.leaderboard, player))
        }
        player.sendMessage(MESSAGES.get("leaderboard.update-rank-message").replaceText {
            it.matchLiteral("%newRank%").replacement("#${event.newRank}")
        }.replaceText {
            it.matchLiteral("%oldRank%").replacement("#${event.oldRank}")
        })
    }
}

class LeaderboardPlayerReachTopListener(private val config: FileConfiguration) : Listener {

    @EventHandler
    fun onLeaderboardPlayerReachTop(event: LeaderboardPlayerReachTopEvent) {
        val eventPlayer = event.player
        val animationCooldown = event.leaderboard.topAnimationCooldown
        if (animationCooldown.containsKey(eventPlayer.uniqueId)) {
            if ((animationCooldown[eventPlayer.uniqueId] ?: 0L) > System.currentTimeMillis())
                return
        } else {
            animationCooldown[eventPlayer.uniqueId] =
                System.currentTimeMillis() + (config.getInt("leaderboard.reach-top.animation-cooldown") * 1000)
        }

        Bukkit.getOnlinePlayers().forEach { player ->
            if (config.getBoolean("leaderboard.reach-top.title.enabled")) {
                player.showTitle(
                    Title.title(
                        MESSAGES.get("leaderboard.reach-top.title.title")
                            .replaceText { it.matchLiteral("%player%").replacement(eventPlayer.name) },
                        MESSAGES.get("leaderboard.reach-top.title.subtitle")
                            .replaceText { it.matchLiteral("%player%").replacement(eventPlayer.name) },
                        Title.Times.times(
                            Duration.ofSeconds(config.getInt("leaderboard.reach-top.title.fadeIn").toLong()),
                            Duration.ofSeconds(config.getInt("leaderboard.reach-top.title.stay").toLong()),
                            Duration.ofSeconds(config.getInt("leaderboard.reach-top.title.fadeOut").toLong())
                        )
                    )
                )
            }

            if (config.getBoolean("leaderboard.reach-top.broadcast.enabled"))
                player.sendMessage(
                    MESSAGES.get("leaderboard.reach-top.broadcast.message")
                        .replaceText { it.matchLiteral("%player%").replacement(eventPlayer.name) }
                )

            if (config.getBoolean("leaderboard.reach-top.sound.enabled"))
                player.playSound(
                    player,
                    config.getString("leaderboard.reach-top.sound.sound") ?: "minecraft:entity.ender_dragon.death",
                    1f,
                    1f
                )

            if (config.getBoolean("leaderboard.reach-top.actionbar.enabled"))
                player.sendActionBar(
                    MESSAGES.get("leaderboard.reach-top.actionbar.message")
                        .replaceText { it.matchLiteral("%player%").replacement(eventPlayer.name) })
        }
    }
}