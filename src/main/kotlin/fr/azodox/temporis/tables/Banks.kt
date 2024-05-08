package fr.azodox.temporis.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

object Banks : IntIdTable("banks") {
    val name = varchar("name", 255).uniqueIndex()
    val uuid = uuid("uuid")
    var emeralds = integer("emeralds").default(0)
}

class Bank(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Bank>(Banks)

    var name by Banks.name
    var uuid by Banks.uuid
    private var emeralds by Banks.emeralds

    /**
     * Deposit emeralds in the bank
     * !! this function only updates the database, it does not remove the emeralds from the player !!
     * @param player the player who deposits the emeralds
     */
    fun deposit(player: Player, depositedEmeralds: Int){
        transaction {
            Player.find { Players.uuid eq player.uuid }.firstOrNull()?.let {
                it.depositedEmeralds += depositedEmeralds
            }
            emeralds += depositedEmeralds
        }
    }

    fun reset(){
        transaction {
            emeralds = 0
        }
    }
}