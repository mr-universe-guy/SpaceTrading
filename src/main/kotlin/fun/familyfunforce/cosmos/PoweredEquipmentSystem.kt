package `fun`.familyfunforce.cosmos

import com.simsilica.es.EntityData
import com.simsilica.es.EntitySet
import com.simsilica.es.Filters
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

class PoweredEquipmentSystem: AbstractGameSystem() {
    private lateinit var data:EntityData
    private lateinit var poweredEquipment:EntitySet

    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        poweredEquipment = data.getEntities(
            Filters.fieldEquals(EquipmentPower::class.java, "powered", true),
//            IsPoweredEquipment::class.java,
            CycleTimer::class.java,
            EquipmentPower::class.java,
            Parent::class.java
        )
    }

    override fun terminate() {
        poweredEquipment.release()
    }

    override fun update(time: SimTime) {
        poweredEquipment.applyChanges()
        val curTime = time.time
        poweredEquipment.forEach {
            val cycle = it.get(CycleTimer::class.java)
            //we only care about active equipment that has completed its cycle
            if(cycle.nextCycle>curTime){ it.set(Activated(false)); return}
            //increment next cycle and activate
            val ct = CycleTimer(time.getFutureTime(cycle.duration), cycle.duration)
            it.set(ct)
//            println("Cycle timer: $ct")
            //create a signal to inform other entities this equipment has activated
            it.set(Activated(true))
        }
    }
}