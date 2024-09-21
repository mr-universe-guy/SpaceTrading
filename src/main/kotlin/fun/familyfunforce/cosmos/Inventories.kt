package `fun`.familyfunforce.cosmos

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.EntitySet
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import `fun`.familyfunforce.cosmos.systems.DataSystem

//The currently active item database, atm this is dangerous as it relies on being set from outside
//late init var activeDatabase: ItemDatabase

data class ItemId(val id: String)

/**
 * Items are the basis of all trade and crafting. Items are identified by their unique id. Volume is in cubic meters.
 * Display names do NOT have to be unique!
 */
data class Item(val id: ItemId, val displayName: String, val volume: Double)

enum class TransactionStatus{
    SUCCESS,
    NOT_ENOUGH_SPACE,
    NOT_ENOUGH_ITEMS,
    ITEM_ID_NOT_FOUND,
    INVENTORY_ID_NOT_FOUND
}

//TODO: Don't store the database reference in the inventory, pass it as a parameter
data class Inventory(val id:Long, var maxVolume: Double, val items: MutableMap<ItemId, Long>, private val database: ItemDatabase){
    //TODO: only update current volume when inventory changes
    var currentVolume: Double = calculateCurrentVolume()

    constructor(id:Long, maxVolume: Double, database: ItemDatabase) : this(id, maxVolume, mutableMapOf(), database)

    private fun calculateCurrentVolume(): Double{
        return items.entries.sumOf{
            val item = database.getItem(it.key) ?: return 0.0
            item.volume * it.value
        }
    }

    fun canAddItem(incomingItem: ItemId, quantity: Long): TransactionStatus{
        val item = database.getItem(incomingItem) ?: return TransactionStatus.ITEM_ID_NOT_FOUND
        return if(currentVolume + (item.volume*quantity) > maxVolume){
            TransactionStatus.NOT_ENOUGH_SPACE
        } else{
            TransactionStatus.SUCCESS
        }
    }

    fun canAddItems(incomingItems: Map<ItemId, Long>): TransactionStatus{
        //first we need to ensure all the items can fit
        val incomingVolume = incomingItems.entries.sumOf {
            val item = database.getItem(it.key) ?: return TransactionStatus.ITEM_ID_NOT_FOUND
            item.volume * it.value
        }
        return if(incomingVolume + currentVolume > maxVolume){
            TransactionStatus.NOT_ENOUGH_SPACE
        } else {
            TransactionStatus.SUCCESS
        }
    }

    fun canRemoveItem(outgoingItem: ItemId, quantity: Long): TransactionStatus{
        return if((items[outgoingItem] ?: 0) < quantity){
            TransactionStatus.NOT_ENOUGH_ITEMS
        } else{
            TransactionStatus.SUCCESS
        }
    }

    fun canRemoveItems(outgoingItems: Map<ItemId, Long>): TransactionStatus{
        //first ensure we have all the requested items
        return if(outgoingItems.any { (id, qty) -> (items[id] ?: 0) < qty}){
            TransactionStatus.NOT_ENOUGH_ITEMS
        } else{
            TransactionStatus.SUCCESS
        }
    }

    /**
     * Adds the map of Items to this inventory.
     * @param incomingItems A map of ItemId's to the quantity of each to be added
     */
    fun addItems(incomingItems: Map<ItemId, Long>){
        incomingItems.forEach { (id, qty) ->
            items[id] = (items.getOrDefault(id, 0) + qty)
        }
        currentVolume = calculateCurrentVolume()
    }

    fun addItem(itemId: ItemId, quantity: Long){
        items[itemId] = (items.getOrDefault(itemId, 0) + quantity)
        currentVolume = calculateCurrentVolume()
    }

    /**
     * Removes the map of Items from this inventory
     * @param outgoingItems A map of ItemId's to the quantity of each to be removed
     */
    fun removeItems(outgoingItems: Map<ItemId, Long>){
        outgoingItems.forEach { (id, qty) ->
            val remainder = items[id]!! - qty
            if(remainder > 0){
                items[id] = remainder
            } else{
                items.remove(id)
            }
        }
        currentVolume = calculateCurrentVolume()
    }

    fun removeItem(outgoingItem: ItemId, quantity: Long){
        val remainder = items[outgoingItem]!! - quantity
        if(remainder > 0){
            items[outgoingItem] = remainder
        } else{
            items.remove(outgoingItem)
        }
        currentVolume = calculateCurrentVolume()
    }
}

// Serializable transaction data, should only be logged when a transaction is completed successfully
data class Transaction(val id:Long, val timeStamp: Long, val originId:Long, val destinationId:Long, val items: Map<ItemId, Long>)

/**
 * Item Database stores references to all individual items found in the current game session.
 */
class ItemDatabase{
    private val itemData = HashMap<ItemId, Item>()

    fun getItem(id:ItemId):Item?{
        return itemData[id]
    }

    fun append(items: List<Item>){
        for(item in items){
            itemData[item.id] = item
        }
    }

    fun clear(){
        itemData.clear()
    }

    fun fromCSV(path: String){
        val rows = CsvReader().readAll(javaClass.getResourceAsStream(path)!!)
        rows.forEach {
            //Read each line as a new item
            val item = Item(ItemId(it[0]), it[1], it[2].toDouble())
            itemData[item.id] = item
        }
    }

    override fun toString(): String {
        return "ItemDatabase(itemData=$itemData)"
    }
}

class InventorySystem: AbstractGameSystem(){
    private val inventories: MutableMap<Long, Inventory> = mutableMapOf()
    private lateinit var data: EntityData
    private lateinit var database: ItemDatabase
    private lateinit var cargoHolds: EntitySet

    override fun initialize() {
        val dataSystem = getSystem(DataSystem::class.java)
        data = dataSystem.entityData
        database = dataSystem.itemData
        cargoHolds = data.getEntities(CargoHold::class.java)
        cargoHolds.applyChanges()
        cargoHolds.forEach {
            val volume = it.get(CargoHold::class.java).maxVolume
            createInventoryForEntity(it.id.id, volume)
        }
    }

    override fun terminate() {
        cargoHolds.release()
    }

    override fun update(time: SimTime?) {
        if(cargoHolds.applyChanges()){
            for(entity in cargoHolds.addedEntities){
                val eid = entity.id.id
                val hold = entity.get(CargoHold::class.java)
                createInventoryForEntity(eid, hold.maxVolume)
            }
            for (entity in cargoHolds.changedEntities){
                val eid = entity.id.id
                val hold = entity.get(CargoHold::class.java)
                inventories[eid]!!.maxVolume = hold.maxVolume
            }
            for (entity in cargoHolds.removedEntities){
                val eid = entity.id.id
                inventories.remove(eid)
            }
        }
    }

    private fun createInventoryForEntity(eid: Long, volume: Double){
        inventories.getOrPut(eid) { Inventory(eid, volume, database) }
        println("$eid has created an inventory")
    }

    fun requestAddItem(targetId: Long, item: ItemId, quantity: Long): TransactionStatus{
        val inventory = inventories[targetId] ?: return TransactionStatus.INVENTORY_ID_NOT_FOUND
        val canAdd = inventory.canAddItem(item, quantity)
        if(canAdd != TransactionStatus.SUCCESS) return canAdd
        inventory.addItem(item, quantity)
        data.setComponent(EntityId(targetId), Cargo(inventory.currentVolume))
        return TransactionStatus.SUCCESS
    }
}