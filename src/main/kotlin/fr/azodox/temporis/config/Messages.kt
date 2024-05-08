package fr.azodox.temporis.config

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.file.FileConfiguration

class Messages(private val config: FileConfiguration) {

    fun get(key: String): Component {
        return MiniMessage.miniMessage().deserialize(config.getString(key) ?: "Inconnu")
    }
}