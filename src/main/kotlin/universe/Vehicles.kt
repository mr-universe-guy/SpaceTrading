package universe

import com.jme3.asset.AssetInfo
import com.jme3.asset.AssetKey
import com.jme3.asset.AssetLoader
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.lang.Exception

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

/**
 * A cache containing all equipment, stored by EquipmentId
 */
private val EQUIPMENT_CACHE = mutableMapOf<String, Equipment>()

/**
 * Loadouts assign equipment to section slots of a specific vehicle.
 * @param vehicleId The name of the vehicle this loadout is modifying
 */
@Serializable
data class Loadout(var name: String, val vehicleId: String){
    private val equipmentMap: MutableMap<String, MutableList<String>> = HashMap()
    @Transient
    private val vehicle: Vehicle = VEHICLE_CACHE[vehicleId]!!

    init{
        if(equipmentMap.isEmpty()){
            vehicle.sections.forEach { (s) -> equipmentMap[s] = mutableListOf() }
        }
    }

    fun getEquipment(): Map<String, List<Equipment?>> {
        return equipmentMap.mapValues { it.value.map { id -> EQUIPMENT_CACHE[id] } }
    }

    fun attachEquipment(sect: String, equipment: Equipment){
        if(!canEquip(sect, equipment.equipmentId)) throw Exception("$equipment can not fit in ${vehicle.sections[sect]}")
        equipmentMap[sect]!!.add(equipment.equipmentId)
    }

    fun getFreeSlots(section:Section): Int{
        var slots = section.slots
        equipmentMap[section.name]!!.map{EQUIPMENT_CACHE[it]!!}.forEach {slots-= it.size}
        return slots
    }

    fun getFreeSlots(sect: String): Int{
        return getFreeSlots(vehicle.sections[sect]!!)
    }

    fun canEquip(sect: String, equipmentId: String): Boolean{
        val section = vehicle.sections[sect]!!
        val equipment = EQUIPMENT_CACHE[equipmentId]!!
        //this section and this equipment exist, now see if they fit
        //get remaining space in section
        val freeSlots = getFreeSlots(section)
        if(freeSlots < equipment.size) return false
        //check bays and other stuff
        return true
    }
}

/**
 * The base model for a vehicle, ie ships, stations, drones, etc.
 * Vehicles are made up of many sections
 */
@Serializable
data class Vehicle(val name: String, val vehicleId: String, val basePower: Int, val emptyMass: Double, val asset: String, val category: Category,
              val sections: MutableMap<String, Section>){
    constructor(name:String, vehicleId: String, basePower: Int, emptyMass: Double, asset: String, category: Category, _sections:Array<Section>)
            : this(name, vehicleId, basePower, emptyMass, asset, category, HashMap()) {
        _sections.forEach { sections[it.name] = it }
    }
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

@Serializable
enum class EquipmentType{
    WEAPON, ENGINE, CARGO, ENERGY, SENSOR
}

/**
 * Equipment is anything that can be installed into a Section.
 * There is functional and passive equipment.
 * Equipment must be registered with it's cache, it is not automatic like the Vehicle cache
 * TODO: Equipment should handle it's own accumulation and component creation
 */
abstract class Equipment(){
    abstract val equipmentId: String
    abstract val name:String
    abstract val equipmentType:EquipmentType
    abstract val size:Int
    abstract val power:Int
}

/**
 * Adds this piece of equipment to the cache
 */
fun cacheEquipment(equipment: Equipment){
    EQUIPMENT_CACHE[equipment.equipmentId] = equipment
}

/**
 * A piece of equipment that directly applies thrust
 */
@Serializable
data class EngineEquip(override val equipmentId: String, override val name: String, override val size:Int, override val power:Int,
                       val maxSpeed: Double, val maxThrust: Double): Equipment(){
    override val equipmentType = EquipmentType.ENGINE
}

/**
 * A piece of equipment that directly stores cargo
 */
@Serializable
data class CargoEquip(override val equipmentId: String, override val name: String, override val size:Int, override val power: Int,
                      val volume:Double): Equipment(){
    override val equipmentType = EquipmentType.CARGO
}

/**
 * A piece of equipment that directly generates and stores energy
 */
@Serializable
data class EnergyGridEquip(override val equipmentId: String, override val name: String, override val size:Int, override val power: Int,
                           val storage: Long, val recharge: Long, val cycleTime: Double): Equipment(){
    override val equipmentType = EquipmentType.ENERGY
}

@Serializable
data class SensorEquip(override val equipmentId: String, override val name: String, override val size:Int, override val power: Int,
                       val range:Double): Equipment(){
    override val equipmentType = EquipmentType.SENSOR
}

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

class EquipmentKey(name: String): AssetKey<Equipment>(name)

/**
 * Simple json equipment loader that caches the equipment as it is loaded
 */
class EquipmentLoader: AssetLoader{
    override fun load(assetInfo: AssetInfo): Any {
        val assetString = assetInfo.openStream().bufferedReader().use{it.readText()}
        val equip = VEHICLE_FORMAT.decodeFromString<Equipment>(assetString)
        cacheEquipment(equip)
        return equip
    }
}