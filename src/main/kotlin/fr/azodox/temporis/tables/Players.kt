package fr.azodox.temporis.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Players : IntIdTable("players") {
    val uuid = uuid("uuid").uniqueIndex()
    var depositedEmeralds = long("deposited_emeralds").default(0)
}

class Player(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Player>(Players)

    var uuid by Players.uuid
    var depositedEmeralds by Players.depositedEmeralds
}