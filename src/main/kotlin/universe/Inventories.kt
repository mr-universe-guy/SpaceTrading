package universe
//The currently active item database, atm this is dangerous as it relies on being set from outside
lateinit var activeDatabase: ItemDatabase

/**
 * Items are the basis of all trade and crafting. Items are identified by their unique id. Volume is in cubic meters.
 * Display names do NOT have to be unique!
 */
data class Item(val id: String, val displayName: String, val volume: Double)

/**
 * Item Stacks specify an item and a quantity.
 */
data class ItemStack(val itemId:String, val itemQuantity:Long){
    //TODO: Probably not safe to leave this as is without ItemDatabase being a singleton or similar
    override fun toString(): String {
        return "ItemStack[%s, %d]".format(activeDatabase.getItem(itemId), itemQuantity)
    }
}

/**
 * Item Database stores references to all individual items found in the current game session.
 */
class ItemDatabase{
    private val itemData = HashMap<String, Item>()

    init{
        //TODO: This is a very janky way to get some free debug info, definitely needs changed in the future!
        activeDatabase=this
    }

    fun getItem(id:String):Item?{
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
}