package universe

import com.jme3.asset.AssetInfo
import com.jme3.asset.AssetKey
import com.jme3.asset.AssetLoader
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Polymorphic serializer for vehicle
 */
private val VEHICLE_MOD = SerializersModule {
    polymorphic(Equipment::class){
        subclass(EngineEquip::class, EngineEquip.serializer())
        subclass(CargoEquip::class, CargoEquip.serializer())
        subclass(EnergyGridEquip::class, EnergyGridEquip.serializer())
        subclass(SensorEquip::class, SensorEquip.serializer())
    }
}

/**
 * Json format for vehicles
 */
val VEHICLE_FORMAT = Json{
    serializersModule = VEHICLE_MOD
    prettyPrint = true
}

/**
 * A cache containing all vehicles, stored by VehicleId
 */
private val VEHICLE_CACHE = mutableMapOf<String, Vehicle>()

fun getVehicleCache(): Map<String, Vehicle>{
    return VEHICLE_CACHE.toMap()
}

/**
 * Load the cached vehicle if it exists
 */
fun getVehicleFromId(id: String): Vehicle?{
    return VEHICLE_CACHE[id]
}

/**
 * The base model for a vehicle, ie ships, stations, drones, etc.
 * Vehicles are made up of many sections
 */
@Serializable
data class Vehicle(val name: String, val vehicleId: String, val basePower: Int, val asset: String,
                   val category: Category, val stats: Map<String,@Contextual Any>, val sections: MutableMap<String, Section>) {
    constructor(_name:String, _vehicleId:String, _basePower:Int, _asset:String, _category:Category, _stats:Map<String, Any>,
                _sections:Array<Section>): this(_name, _vehicleId, _basePower, _asset, _category, _stats, (_sections.associateBy{it.name}).toMutableMap())
    init{
        VEHICLE_CACHE[vehicleId] = this
    }
}

/**
 * Vehicles are made up of many sections. Each section has slots that equipment can be installed in.
 * Some equipment require a Bay to be functional.
 */
@Serializable
data class Section(val name:String, val slots: Int, val HP: Long, val bays: List<Bay>?)

/**
 * Bays separate a number of slots that can only fit equipment of a certain type.
 */
@Serializable
data class Bay(val count: Int, val types: Set<EquipmentType>)

/**
 * An asset key to easily load vehicles via the asset manager
 */
class VehicleKey(name: String): AssetKey<Vehicle>(name)

/**
 * An asset loader to read vehicle assets from Json input
 */
class VehicleLoader: AssetLoader {
    override fun load(assetInfo: AssetInfo): Any {
        //get asset as string
        val assetString = assetInfo.openStream().bufferedReader().use { it.readText() }
        return VEHICLE_FORMAT.decodeFromString<Vehicle>(assetString)
    }
}