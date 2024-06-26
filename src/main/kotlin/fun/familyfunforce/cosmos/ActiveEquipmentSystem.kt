package `fun`.familyfunforce.cosmos

import com.simsilica.es.EntityData
import com.simsilica.es.EntitySet
import com.simsilica.es.Filters
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

class ActiveEquipmentSystem: AbstractGameSystem() {
    private lateinit var data:EntityData
    private lateinit var actives:EntitySet

    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        actives = data.getEntities(
            Filters.fieldEquals(Activated::class.java, "active", true), IsActiveEquipment::class.java, CycleTimer::class.java, EquipmentPower::class.java,
            Parent::class.java, Activated::class.java)
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
            if(!it.get(EquipmentPower::class.java).active || cycle.nextCycle>curTime){ it.set(Activated(false)); return}
            //increment next cycle and activate
            val ct = CycleTimer(time.getFutureTime(cycle.duration), cycle.duration)
            it.set(ct)
            //println("Cycle timer: $ct")
            //create a signal to inform other entities this equipment has activated
            it.set(Activated(true))
        }
    }
}