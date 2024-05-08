package fr.azodox.temporis.listener

import fr.azodox.temporis.LEADERBOARD
import fr.azodox.temporis.MESSAGES
import fr.azodox.temporis.Temporis
import fr.azodox.temporis.events.LeaderboardEntriesUpdateEvent
import fr.azodox.temporis.tables.Bank
import fr.azodox.temporis.tables.Banks
import fr.azodox.temporis.tables.Players
import fr.azodox.temporis.util.LocationSerialization
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.Plugin
import org.jetbrains.exposed.sql.transactions.transaction

class PlayerInteractWithSignListener(private val temporis: Temporis) : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        event.clickedBlock?.let {
            if (event.hand == EquipmentSlot.OFF_HAND)
                return
            if (it.state !is Sign)
                return
            if (!temporis.config.getStringList("emeraldSigns").contains(LocationSerialization.serialize(it.location)))
                return

            val inventory = Bukkit.createInventory(
                null,
                5 * 9,
                Component.text("Dépôt").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
            )
            event.player.openInventory(inventory)
            event.isCancelled = true
        }
    }
}

class PlayerCloseInventory(private val plugin: Plugin) : Listener {

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        if (event.view.title() != Component.text("Dépôt").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
            return

        val inventory = event.inventory
        if (inventory.contents.none { it != null && it.type != Material.AIR })
            return

        val oldEntries = LEADERBOARD.entries.toMutableList()
        transaction {
            Bank.find { Banks.name eq "default" }.firstOrNull()?.deposit(
                fr.azodox.temporis.tables.Player.find { Players.uuid eq event.player.uniqueId }.first(),
                foundAmountOfEmeralds(inventory)
            )
        }
        inventory.contents.forEach {
            if (it != null && it.type != Material.AIR)
                event.player.world.dropItem(event.player.location, it)
        }
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            Bukkit.getPluginManager().callEvent(LeaderboardEntriesUpdateEvent(LEADERBOARD, oldEntries))
        }, 20)
        event.player.sendMessage(MESSAGES.get("deposit.success"))
    }

    private fun foundAmountOfEmeralds(inventory: Inventory): Int {
        var amount = 0
        inventory.contents.filterNotNull().filter { it.type != Material.AIR }
            .filter { it.type == Material.EMERALD || it.type == Material.EMERALD_BLOCK }.forEach {
                amount += if (it.type == Material.EMERALD_BLOCK)
                    it.amount * 9
                else
                    it.amount
                inventory.remove(it)
            }
        return amount
    }
}
