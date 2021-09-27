package universe

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Loadouts assign equipment to section slots of a specific vehicle.
 * @param vehicleId The name of the vehicle this loadout is modifying
 */
@Serializable
data class Loadout(var name: String, val vehicleId: String){
    private val equipmentMap: MutableMap<String, MutableList<String>> = HashMap()
    @Transient
    private val vehicle: Vehicle = getVehicleFromId(vehicleId)!!

    init{
        if(equipmentMap.isEmpty()){
            vehicle.sections.forEach { (s) -> equipmentMap[s] = mutableListOf() }
        }
    }

    fun getEquipment(): Map<String, List<Equipment?>> {
        return equipmentMap.mapValues { it.value.map { id -> getEquipmentFromId(id) } }
    }

    /**
     * Attempts to put the equipment into this slot
     * @return true if equipment was placed successfully, false otherwise
     */
    fun attachEquipment(sect: String, equipment: Equipment): Boolean{
        if(!canEquip(sect, equipment.equipmentId)) return false
        equipmentMap[sect]!!.add(equipment.equipmentId)
        return true
    }

    fun removeEquipment(sect: String, equipment: String): Boolean{
        return equipmentMap[sect]!!.remove(equipment)
    }

    fun getFreeSlots(section:Section): Int{
        var slots = section.slots
        equipmentMap[section.name]!!.map{ getEquipmentFromId(it)!!}.forEach {slots-= it.size}
        return slots
    }

    fun canEquip(sect: String, equipmentId: String): Boolean{
        val section = vehicle.sections[sect]!!
        val equipment = getEquipmentFromId(equipmentId)!!
        //this section and this equipment exist, now see if they fit
        //get remaining space in section
        val freeSlots = getFreeSlots(section)
        if(freeSlots < equipment.size) return false
        //check bays and other stuff
        return true
    }

    fun getEquipmentInSection(sect: String): List<String>{
        return equipmentMap[sect]!!.toList()
    }

    fun getStats(): Map<String, Any>{
        //sum all equipment, apply modifiers, etc
        val curStats = vehicle.stats.toMutableMap()
        equipmentMap.values.forEach { sect ->
            sect.forEach {
                val equipment = getEquipmentFromId(it)
                equipment!!.getModifiedStats(curStats, this)
            }
        }
        return curStats
    }
}

fun exportLoadout(loadout: Loadout, file: File){
    file.writeText(Json.encodeToString(loadout))
}