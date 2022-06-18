package `fun`.familyfunforce.cosmos

import `fun`.familyfunforce.cosmos.loadout.ActiveEquipment
import `fun`.familyfunforce.cosmos.loadout.getEquipmentFromId
import com.simsilica.es.EntityData
import com.simsilica.es.EntitySet
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

class ActiveEquipmentSystem: AbstractGameSystem() {
    private lateinit var data:EntityData
    private lateinit var actives:EntitySet

    override fun initialize() {
        data = getSystem(DataSystem::class.java).getPhysicsData()
        actives = data.getEntities(EquipmentAsset::class.java, CycleTimer::class.java, Activate::class.java, Parent::class.java)
    }

    override fun terminate() {
        actives.release()
    }

    override fun update(time: SimTime) {
        actives.applyChanges()
        val curTime = time.time
        actives.forEach {
            val cycle = it.get(CycleTimer::class.java)
            //we only care about active equipment that has completed its cycle
            if(!it.get(Activate::class.java).active || cycle.nextCycle>curTime) return
            //increment next cycle and activate
            val equip = getEquipmentFromId(it.get(EquipmentAsset::class.java).equipmentId)
            //TODO: do this better?
            if(equip !is ActiveEquipment) throw Exception("Equipment $equip is not an active equipment")
            it.set(CycleTimer(time.getFutureTime(cycle.duration), cycle.duration))
            equip.activate(it.get(Parent::class.java)!!.parentId, data)
        }
    }
}