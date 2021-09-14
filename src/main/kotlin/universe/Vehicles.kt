package universe

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

val mod = SerializersModule {
    polymorphic(Equipment::class){
        subclass(EngineEquip::class, EngineEquip.serializer())
        subclass(CargoEquip::class, CargoEquip.serializer())
        subclass(EnergyGridEquip::class, EnergyGridEquip.serializer())
        subclass(SensorEquip::class, SensorEquip.serializer())
    }
}

/**
 * Loadouts assign equipment to section slots of a specific vehicle.
 * @param vehicleKey The name of the vehicle this loadout is modifying
 */
@Serializable
class Loadout(val vehicleKey: String){
    val equipmentMap: MutableMap<String, MutableList<Equipment?>> = HashMap()

    fun attachEquipment(sect: String, equipment: Equipment){
        equipmentMap[sect]!!.add(equipment)
    }
}

/**
 * The base model for a vehicle, ie ships, stations, drones, etc.
 * Vehicles are made up of many sections
 */
@Serializable
class Vehicle(val name: String, val basePower: Int, val emptyMass: Double, val asset: String, val category: Category,
              val sections: MutableMap<String, Section>){
    constructor(name:String, basePower: Int, emptyMass: Double, asset: String, category: Category, _sections:Array<Section>)
            : this(name, basePower, emptyMass, asset, category, HashMap()) {
        _sections.forEach { sections[it.name] = it }
    }
}

/**
 * Vehicles are made up of many sections. Each section has slots that equipment can be installed in.
 * Some equipment require a Bay to be functional.
 */
@Serializable
data class Section(val name:String, val slots: Int, val HP: Long, val bays: Array<Bay>?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Section

        if (name != other.name) return false
        if (slots != other.slots) return false
        if (HP != other.HP) return false
        if (bays != null) {
            if (other.bays == null) return false
            if (!bays.contentEquals(other.bays)) return false
        } else if (other.bays != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + slots
        result = 31 * result + HP.hashCode()
        result = 31 * result + (bays?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Bays separate a number of slots that can only fit equipment of a certain type.
 */
@Serializable
data class Bay(val count: Int, val types: Array<EquipmentType>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bay

        if (count != other.count) return false
        if (!types.contentEquals(other.types)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = count
        result = 31 * result + types.contentHashCode()
        return result
    }
}

@Serializable
enum class EquipmentType{
    WEAPON, ENGINE, CARGO, ENERGY, SENSOR
}

/**
 * Equipment is anything that can be installed into a Section.
 * There is functional and passive equipment.
 * TODO: Equipment should handle it's own accumulation and component creation
 */
interface Equipment{
    val name:String
    val equipmentType:EquipmentType
    val size:Int
    val power:Int
}

/**
 * A piece of equipment that directly applies thrust
 */
@Serializable
class EngineEquip(override val name: String, override val size:Int, override val power:Int, val maxSpeed: Double, val maxThrust: Double): Equipment{
    override val equipmentType = EquipmentType.ENGINE
}

/**
 * A piece of equipment that directly stores cargo
 */
@Serializable
class CargoEquip(override val name: String, override val size:Int, override val power: Int, val volume:Double): Equipment{
    override val equipmentType = EquipmentType.CARGO
}

/**
 * A piece of equipment that directly generates and stores energy
 */
@Serializable
class EnergyGridEquip(override val name: String, override val size:Int, override val power: Int, val storage: Long,
                      val recharge: Long, val cycleTime: Double): Equipment{
    override val equipmentType = EquipmentType.ENERGY
}

@Serializable
class SensorEquip(override val name: String, override val size:Int, override val power: Int, val range:Double): Equipment{
    override val equipmentType = EquipmentType.SENSOR
}