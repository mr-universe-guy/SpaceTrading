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
            CycleTimer::class.java,
            EquipmentPower::class.java,
            Parent::class.java,
            ActivationConsumed::class.java
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
            //first check if this activation has been consumed
            if(it.get(ActivationConsumed::class.java).consumed){
                data.setComponents(it.id,
                    CycleTimer(time.getFutureTime(cycle.duration), cycle.duration),
                    Activated(false),
                    ActivationConsumed(false)
                )
            } else{
                if(cycle.nextCycle<=curTime){
                    it.set(Activated(true))
                }
            }
        }
    }
}