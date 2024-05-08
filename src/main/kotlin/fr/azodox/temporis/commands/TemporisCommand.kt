package fr.azodox.temporis.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.HelpCommand
import co.aikar.commands.annotation.Subcommand
import fr.azodox.temporis.BOARDS
import fr.azodox.temporis.MESSAGES
import fr.azodox.temporis.Temporis
import fr.azodox.temporis.config.Messages
import fr.azodox.temporis.tables.Bank
import fr.azodox.temporis.util.LocationSerialization
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.Sign
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.transaction

@CommandAlias("temporis")
@CommandPermission("temporis.admin")
class TemporisCommand(private val temporis: Temporis) : BaseCommand() {

    @Subcommand("reload")
    @CommandPermission("temporis.reload")
    fun onReload(sender: CommandSender) {
        sender.sendMessage(Component.text("Rechargement de Temporis...").color(NamedTextColor.GRAY))
        temporis.reloadConfig()
        temporis.reloadLeaderboard()
        temporis.reloadLeaderboardTask()
        MESSAGES = Messages(temporis.config)
        sender.sendMessage(Component.text("- Leaderboard rechargé !").color(NamedTextColor.GRAY))

        BOARDS.values.forEach { board ->
            board.updateTitle(MESSAGES.get("scoreboard.title"))
        }
        sender.sendMessage(Component.text("- Scoreboard rechargé !").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("Temporis rechargé !").color(NamedTextColor.GREEN))
    }

    @Subcommand("signs add")
    @CommandPermission("temporis.signs.add")
    fun addSign(player: Player) {
        player.getTargetBlockExact(5)?.let {
            if(it.state is Sign) {
                val signs = temporis.config.getStringList("emeraldSigns")
                val serializedLoc = LocationSerialization.serialize(it.location)
                if(signs.contains(serializedLoc)) {
                    player.sendMessage(Component.text("Erreur : ce panneau est déjà enregistré !").color(NamedTextColor.RED))
                    return
                }

                signs.add(serializedLoc)
                temporis.config.set("emeraldSigns", signs)
                temporis.saveConfig()
                player.sendMessage(Component.text("Panneau ajouté !").color(NamedTextColor.GREEN))
            } else
                player.sendMessage(Component.text("Erreur : le bloc visé n'est pas un panneau !").color(NamedTextColor.RED))
        }
    }

    @Subcommand("signs remove")
    @CommandPermission("temporis.signs.remove")
    fun removeSign(player: Player) {
        player.getTargetBlockExact(5)?.let {
            if(it.state is Sign) {
                val signs = temporis.config.getStringList("emeraldSigns")
                if (!signs.remove(LocationSerialization.serialize(it.location)))
                    player.sendMessage(Component.text("Erreur : ce panneau n'est pas enregistré !").color(NamedTextColor.RED))
                temporis.config.set("emeraldSigns", signs)
                temporis.saveConfig()
                player.sendMessage(Component.text("Panneau supprimé !").color(NamedTextColor.GREEN))
            } else
                player.sendMessage(Component.text("Erreur : le bloc visé n'est pas un panneau !").color(NamedTextColor.RED))
        }
    }

    @Subcommand("bank reset")
    @CommandPermission("temporis.bank.reset")
    fun resetBank(sender: CommandSender) {
        transaction {
            Bank.all().forEach(Bank::reset)
            fr.azodox.temporis.tables.Player.all().forEach { it.depositedEmeralds = 0 }
            sender.sendMessage(Component.text("Banque réinitialisée !").color(NamedTextColor.GREEN))
        }
    }

    @HelpCommand
    fun onHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("Erreur : /temporis <reload|signs>").color(NamedTextColor.RED))
    }
}
