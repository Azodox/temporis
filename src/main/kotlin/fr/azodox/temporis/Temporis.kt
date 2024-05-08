package fr.azodox.temporis

import co.aikar.commands.PaperCommandManager
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fr.azodox.temporis.commands.TemporisCommand
import fr.azodox.temporis.config.Messages
import fr.azodox.temporis.leaderboard.Leaderboard
import fr.azodox.temporis.listener.*
import fr.azodox.temporis.tables.Bank
import fr.azodox.temporis.tables.Banks
import fr.azodox.temporis.tables.Player
import fr.azodox.temporis.tables.Players
import fr.azodox.temporis.task.TabTask
import fr.azodox.temporis.util.LocationSerialization
import fr.mrmicky.fastboard.adventure.FastBoard
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


lateinit var LEADERBOARD: Leaderboard
lateinit var LEADERBOARD_TASK: BukkitTask
lateinit var MESSAGES: Messages

var BOARDS = mutableMapOf<UUID, FastBoard>()

class Temporis : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()
        MESSAGES = Messages(config)

        initSQL()
        initLeaderboard()

        server.pluginManager.registerEvents(LeaderboardEntriesUpdateListener(), this)
        server.pluginManager.registerEvents(LeaderboardPlayerRankUpdatedListener(), this)
        server.pluginManager.registerEvents(LeaderboardPlayerReachTopListener(config), this)

        LEADERBOARD_TASK = server.scheduler.runTaskTimerAsynchronously(this, Runnable {
            LEADERBOARD.updateEntries()
        }, 0, config.getInt("leaderboard.updateInterval").toLong())

        server.pluginManager.registerEvents(PlayerJoinListener(this), this)
        server.pluginManager.registerEvents(PlayerInteractWithSignListener(this), this)
        server.pluginManager.registerEvents(PlayerCloseInventory(this), this)


        val commandManager = PaperCommandManager(this)
        commandManager.registerCommand(TemporisCommand(this))

        server.onlinePlayers.forEach(::initBoard)

        server.scheduler.runTaskTimerAsynchronously(this, Runnable {
            BOARDS.values.forEach { updateBoard(it) }
        }, 0, 200)

        server.scheduler.runTaskTimerAsynchronously(this, TabTask, 0, 200)
        getLogger().info("Enabled!")
    }

    override fun onDisable() {
        LEADERBOARD.delete()
        getLogger().info("Disabled!")
    }

    fun initBoard(player: org.bukkit.entity.Player) {
        val board = FastBoard(player)
        board.updateTitle(MESSAGES.get("scoreboard.title"))
        BOARDS[player.uniqueId] = board
    }

    private fun initLeaderboard() {
        LEADERBOARD = Leaderboard(
            this,
            MESSAGES.get("leaderboard.displayName"),
            LocationSerialization.deserialize(config.getString("leaderboard.topLocation")),
            config.getInt("leaderboard.limit"),
            config.getDouble("leaderboard.entriesGap")
        )
    }

    fun reloadLeaderboard() {
        LEADERBOARD.delete()
        initLeaderboard()
    }

    fun reloadLeaderboardTask() {
        LEADERBOARD_TASK.cancel()
        server.scheduler.runTaskTimer(this, Runnable {
            LEADERBOARD.updateEntries()
        }, 0, config.getInt("leaderboard.updateInterval").toLong())
    }

    private fun updateBoard(board: FastBoard) {
        transaction {
            val player = Player.find { Players.uuid eq board.player.uniqueId }.firstOrNull() ?: return@transaction
            board.updateLines(
                Component.space(),
                Component.text("Émeraudes déposées").color(NamedTextColor.GREEN)
                    .append(Component.text(" » ").color(NamedTextColor.DARK_GRAY))
                    .append(Component.text(player.depositedEmeralds).color(NamedTextColor.WHITE)),
                Component.space()
            )
        }
    }

    private fun initSQL() {
        val config = HikariConfig().apply {
            jdbcUrl =
                "jdbc:mysql://${config.getString("mysql.host")}:${config.getInt("mysql.port")}/${config.getString("mysql.database")}"
            driverClassName = "com.mysql.cj.jdbc.Driver"
            username = config.getString("mysql.username")
            password = config.getString("mysql.password")
            maximumPoolSize = config.getInt("mysql.pool.maximumPoolSize")
            connectionTimeout = config.getLong("mysql.pool.connectionTimeout")
            idleTimeout = config.getLong("mysql.pool.idleTimeout")
        }
        val datasource = HikariDataSource(config)
        Database.connect(datasource)

        transaction {
            SchemaUtils.create(Players)
            SchemaUtils.create(Banks)

            if(Bank.all().count { it.name == "default" } == 0) {
                Bank.new {
                    name = "default"
                    uuid = UUID.randomUUID()
                }
            }
        }
    }
}
