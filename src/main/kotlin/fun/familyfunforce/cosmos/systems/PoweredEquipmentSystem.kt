package `fun`.familyfunforce.cosmos.systems

import com.simsilica.es.EntityData
import com.simsilica.es.EntitySet
import com.simsilica.es.Filters
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import `fun`.familyfunforce.cosmos.*

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
            ActivationConsumed::class.java,
            Heat::class.java
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
                //accumulate heat
                data.setComponents(
                    data.createEntity(),
                    HeatChange(it.get(Heat::class.java).heat),
                    Decay(time.getFutureTime(1.0), 1.0),
                    TargetId(it.get(Parent::class.java).parentId)
                )
            } else{
                if(cycle.nextCycle<=curTime){
                    it.set(Activated(true))
                }
            }
        }
    }
}