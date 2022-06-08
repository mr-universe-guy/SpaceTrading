package `fun`.familyfunforce.cosmos

import `fun`.familyfunforce.cosmos.event.OrbitOrderEvent
import `fun`.familyfunforce.cosmos.event.ThrottleOrderEvent
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.event.EventBus
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

/**
 * Actions are the micro level AI decisions performed by units in a fleet. These will need to be very cheap as every fleet
 * that is in simulated space will run one action per unit.
 */
class ActionSystem: AbstractGameSystem() {
    private lateinit var data: EntityData
    private val unitActions = HashMap<EntityId, Action>()

    override fun initialize() {
        data = getSystem(DataSystem::class.java).getPhysicsData()
        EventBus.addListener(this, OrbitOrderEvent.orbitTarget, ThrottleOrderEvent.setThrottle)
    }

    override fun terminate() {
        EventBus.removeListener(this, OrbitOrderEvent.orbitTarget)
    }

    override fun update(time: SimTime) {
        unitActions.forEach { (entityId, action) ->
            //This may not be a great way of doing things when we have a massive number of ships
            val status = action.update(entityId, data, time)
            //if the status has changed assign a new status info
            if(status != action.getStatus()){
                action.setStatus(status)
                data.setComponent(entityId, ActionInfo(action.toString(), status))
            }
        }
    }

    fun setAction(id: EntityId, action: Action?){
        if(action == null){
            unitActions.remove(id)
            data.removeComponent(id, ActionInfo::class.java)
        } else{
            unitActions[id] = action
            data.setComponent(id,
                ActionInfo(
                    action.toString(),
                    ActionStatus.STARTING
                )
            )
        }
    }

    fun orbitTarget(orb: OrbitOrderEvent){
        setAction(orb.shipId, OrbitAction(orb.targetId, orb.range))
    }

    fun setThrottle(evt: ThrottleOrderEvent){
        getAction(evt.shipId)?.setThrottle(evt.throttle)
    }

    fun getAction(id: EntityId): Action?{
        return unitActions[id]
    }
}