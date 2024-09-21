package `fun`.familyfunforce.cosmos.loadout

import com.jme3.asset.AssetInfo
import com.jme3.asset.AssetKey
import com.jme3.asset.AssetLoader
import com.simsilica.es.EntityComponent
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * A cache containing all equipment, stored by EquipmentId
 */
private val EQUIPMENT_CACHE = mutableMapOf<String, Equipment>()

fun getEquipmentCache(): Map<String, Equipment>{
    return EQUIPMENT_CACHE.toMap()
}

fun getEquipmentFromId(id: String): Equipment?{
    return EQUIPMENT_CACHE[id]
}

/**
 * Adds this piece of equipment to the cache
 */
fun cacheEquipment(equipment: Equipment){
    EQUIPMENT_CACHE[equipment.equipmentId] = equipment
}

@Serializable
enum class EquipmentType{
    WEAPON, MINING, ENGINE, CARGO, ENERGY, SENSOR
}

/**
 * Equipment is anything that can be installed into a Section.
 * There is functional and passive equipment.
 * Equipment must be registered with it's cache, it is not automatic like the Vehicle cache
 * TODO: Equipment should handle it's own accumulation and component creation
 */
abstract class Equipment{
    abstract val equipmentId: String
    abstract val name:String
    abstract val equipmentType: EquipmentType
    abstract val size:Int
    abstract val power:Int
}

/**
 * Passive equipment are things that simply modify the ship stats. These don't need to be activated, they simply modify the
 * ship when it spawns or when they are destroyed
 */
abstract class PassiveEquipment: Equipment(){
    /**
     * Get the vehicle stats as they have been modified by this equipment.
     * @inStats Read-only vehicle stats to modify
     * @loadout used to view other equipment present on this vehicle
     * @return
     */
    abstract fun getModifiedStats(inStats: MutableMap<String, Any>, loadout: Loadout)
}

/**
 * Active equipment need to be activated to function. These will create entities when spawned and can be turned on/off
 */
abstract class PoweredEquipment: Equipment(){
    abstract val duration: Double
    abstract val requireTarget: Boolean
    abstract val heat: Int
}

/**
 * Equipment that adds components to the entity that it is attached to
 */
interface ComponentEquipment{val components: List<EntityComponent> }

/**
 * Simple data class to hold a group of subcomponents to be used by other systems
 */
@Serializable
data class ComponentStack(val components: List<EntityComponent>)

class EquipmentKey(name: String): AssetKey<Equipment>(name)

/**
 * Simple json equipment loader that caches the equipment as it is loaded
 */
class EquipmentLoader: AssetLoader {
    override fun load(assetInfo: AssetInfo): Any {
        val assetString = assetInfo.openStream().bufferedReader().use{it.readText()}
        val equip = VEHICLE_FORMAT.decodeFromString<Equipment>(assetString)
        cacheEquipment(equip)
        return equip
    }
}

//**************** Begin Default Equipment ****************//
const val EMPTY_MASS = "EmptyMass"
const val MAX_SPEED = "MaxSpeed"
const val MAX_THRUST = "MaxThrust"
const val CARGO_VOLUME = "CargoVolume"
const val EN_STORAGE = "EnergyStorage"
const val EN_RECHARGE = "EnergyRecharge"
const val EN_CYCLE_TIME = "EnergyCycleTime"
const val SEN_RANGE_MAX = "SensorRangeMax"

/**
 * A piece of equipment that directly applies thrust
 */
@Serializable
data class EngineEquip(override val equipmentId: String, override val name: String, override val size:Int, override val power:Int,
                       val maxSpeed: Double, val maxThrust: Double): PassiveEquipment(){
    override val equipmentType = EquipmentType.ENGINE
    override fun getModifiedStats(inStats: MutableMap<String, Any>, loadout: Loadout) {
        val curMaxSpeed = inStats[MAX_SPEED] as Double? ?: 0.0
        inStats[MAX_SPEED] = curMaxSpeed+maxSpeed
        val curMaxThrust = inStats[MAX_THRUST] as Double? ?: 0.0
        inStats[MAX_THRUST] = curMaxThrust+maxThrust
    }
}

/**
 * A piece of equipment that directly stores cargo
 */
@Serializable
data class CargoEquip(override val equipmentId: String, override val name: String, override val size:Int, override val power: Int,
                      val volume:Double): PassiveEquipment(){
    override val equipmentType = EquipmentType.CARGO
    override fun getModifiedStats(inStats: MutableMap<String, Any>, loadout: Loadout){
        val curCargoCap = inStats[CARGO_VOLUME] as Double? ?: 0.0
        inStats[CARGO_VOLUME] = curCargoCap+volume
    }
}

/**
 * A piece of equipment that directly generates and stores energy
 */
@Serializable
data class EnergyGridEquip(override val equipmentId: String, override val name: String, override val size:Int, override val power: Int,
                           val storage: Long, val recharge: Long, val cycleTime: Double): PassiveEquipment(){
    override val equipmentType = EquipmentType.ENERGY
    override fun getModifiedStats(inStats: MutableMap<String, Any>, loadout: Loadout) {
        val enStorage = inStats[EN_STORAGE] as Long? ?: 0L
        inStats[EN_STORAGE] = enStorage+storage
        val enRefresh = inStats[EN_RECHARGE] as Long? ?: 0L
        inStats[EN_RECHARGE] = enRefresh+recharge
        val enCycle = inStats[EN_CYCLE_TIME] as Double? ?: 0.0
        inStats[EN_CYCLE_TIME] = (enCycle+cycleTime)/2.0
    }
}

@Serializable
data class SensorEquip(
    override val equipmentId: String,
    override val name: String,
    override val size:Int,
    override val power: Int,
    val range:Double
): PassiveEquipment(){
    override val equipmentType = EquipmentType.SENSOR
    override fun getModifiedStats(inStats: MutableMap<String, Any>, loadout: Loadout){
        val senMax = inStats[SEN_RANGE_MAX] as Double? ?: 0.0
        inStats[SEN_RANGE_MAX] = senMax + range
    }
}

@Serializable
data class WeaponEquip(
    override val equipmentId: String,
    override val name:String,
    override val size:Int,
    override val power:Int,
    val cycleTimeMillis:Long,
    val maxRange:Double,
    override val duration: Double,
    override val heat: Int,
    override val components: List<EntityComponent>
):
        PoweredEquipment(), ComponentEquipment{
    override val equipmentType: EquipmentType = EquipmentType.WEAPON
    override val requireTarget: Boolean = true
}

@Serializable
data class MiningEquip(
    override val equipmentId: String,
    override val name:String,
    override val size:Int,
    override val power:Int,
    val cycleTimeMillis:Long,
    val maxRange:Double,
    override val duration: Double,
    override val heat: Int,
    override val components: List<EntityComponent>
): PoweredEquipment(), ComponentEquipment{
    override val equipmentType: EquipmentType = EquipmentType.MINING
    override val requireTarget: Boolean = true
}