package `fun`.familyfunforce.cosmos

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.jme3.network.serializing.Serializer
import com.jme3.network.serializing.serializers.EnumSerializer
import com.jme3.network.serializing.serializers.StringSerializer
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.EntitySet
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import `fun`.familyfunforce.cosmos.systems.DataSystem
import java.nio.ByteBuffer

//The currently active item database, atm this is dangerous as it relies on being set from outside
//late init var activeDatabase: ItemDatabase

object Inventories{
    fun serializeInventories(){
        Serializer.registerClasses(
            ItemId::class.java,
            Item::class.java
        )
        Serializer.registerClass(Inventory::class.java, InventorySerializer())
        Serializer.registerClass(TransactionStatus::class.java, EnumSerializer())
    }
}


@com.jme3.network.serializing.Serializable
data class ItemId(val id: String){
    constructor() : this("")
}

/**
 * Items are the basis of all trade and crafting. Items are identified by their unique id. Volume is in cubic meters.
 * Display names do NOT have to be unique!
 */
@com.jme3.network.serializing.Serializable
data class Item(val id: ItemId, val displayName: String, val volume: Double){
    constructor() : this(ItemId(), "", 0.0)
}

@com.jme3.network.serializing.Serializable
enum class TransactionStatus{
    SUCCESS,
    NOT_ENOUGH_SPACE,
    NOT_ENOUGH_ITEMS,
    ITEM_ID_NOT_FOUND,
    INVENTORY_ID_NOT_FOUND
}

/**
 * Serializer for Inventory since jme serializer did not like how kotlin handles maps
 */
class InventorySerializer: Serializer() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> readObject(data: ByteBuffer, c: Class<T>?): T {
        val id = data.getLong()
        val maxVolume = data.getDouble()
        val currentVolume = data.getDouble()
        val mapSize = data.getInt()
        val map : MutableMap<ItemId, Long> = HashMap(mapSize)
        for(i in 1 .. mapSize){
            val item = ItemId(StringSerializer.readString(data))
            val qty = data.getLong()
            map[item] = qty
        }
        return Inventory(id, maxVolume, map, currentVolume) as T
    }

    override fun writeObject(buffer: ByteBuffer, o: Any?) {
        val inv = o as Inventory
        buffer.putLong(inv.id)
        buffer.putDouble(inv.maxVolume)
        buffer.putDouble(inv.currentVolume)
        //we are only writing ItemId's and
        buffer.putInt(inv.items.size)
        inv.items.forEach {
            val id = it.key.id
            StringSerializer.writeString(id, buffer)
            buffer.putLong(it.value)
        }
    }
}

@com.jme3.network.serializing.Serializable
data class Inventory(
    val id:Long,
    var maxVolume: Double,
    var items: MutableMap<ItemId, Long>,
    var currentVolume: Double){

    constructor(id:Long, maxVolume: Double) : this(id, maxVolume, HashMap(), 0.0)

    constructor() : this(0, 0.0, HashMap(), 0.0)

    private fun calculateCurrentVolume(database: ItemDatabase): Double{
        return items.entries.sumOf{
            val item = database.getItem(it.key) ?: return 0.0
            item.volume * it.value
        }
    }

    fun canAddItem(incomingItem: ItemId, quantity: Long, database: ItemDatabase): TransactionStatus{
        val item = database.getItem(incomingItem) ?: return TransactionStatus.ITEM_ID_NOT_FOUND
        return if(currentVolume + (item.volume*quantity) > maxVolume){
            TransactionStatus.NOT_ENOUGH_SPACE
        } else{
            TransactionStatus.SUCCESS
        }
    }

    fun canAddItems(incomingItems: Map<ItemId, Long>, database: ItemDatabase): TransactionStatus{
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
    fun addItems(incomingItems: Map<ItemId, Long>, database: ItemDatabase){
        incomingItems.forEach { (id, qty) ->
            items[id] = (items.getOrDefault(id, 0) + qty)
        }
        currentVolume = calculateCurrentVolume(database)
    }

    fun addItem(itemId: ItemId, quantity: Long, database: ItemDatabase){
        items[itemId] = (items.getOrDefault(itemId, 0) + quantity)
        currentVolume = calculateCurrentVolume(database)
    }

    /**
     * Removes the map of Items from this inventory
     * @param outgoingItems A map of ItemId's to the quantity of each to be removed
     */
    fun removeItems(outgoingItems: Map<ItemId, Long>, database: ItemDatabase){
        outgoingItems.forEach { (id, qty) ->
            val remainder = items[id]!! - qty
            if(remainder > 0){
                items[id] = remainder
            } else{
                items.remove(id)
            }
        }
        currentVolume = calculateCurrentVolume(database)
    }

    fun removeItem(outgoingItem: ItemId, quantity: Long, database: ItemDatabase){
        val remainder = items[outgoingItem]!! - quantity
        if(remainder > 0){
            items[outgoingItem] = remainder
        } else{
            items.remove(outgoingItem)
        }
        currentVolume = calculateCurrentVolume(database)
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
    lateinit var database: ItemDatabase
    private val inventories: MutableMap<Long, Inventory> = mutableMapOf()
    private lateinit var data: EntityData
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
        inventories.getOrPut(eid) { Inventory(eid, volume) }
        println("$eid has created an inventory")
    }

    fun requestAddItem(targetId: Long, item: ItemId, quantity: Long): TransactionStatus{
        val inventory = inventories[targetId] ?: return TransactionStatus.INVENTORY_ID_NOT_FOUND
        val canAdd = inventory.canAddItem(item, quantity, database)
        if(canAdd != TransactionStatus.SUCCESS) return canAdd
        inventory.addItem(item, quantity, database)
        //This is probably the best way to ensure anyone watching an entity will see inventory changes, I think...
        data.setComponent(EntityId(targetId), Cargo(inventory.currentVolume))
        return TransactionStatus.SUCCESS
    }

    fun getInventoryFromId(targetId: Long): Inventory?{
        return inventories[targetId]
    }
}