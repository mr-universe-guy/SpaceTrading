package universe

import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

/**
 * Actions are the micro level ai decisions performed by units in a fleet. These will need to be very cheap as every fleet
 * that is in simulated space will run one action per unit.
 */
class ActionSystem: AbstractGameSystem() {
    private lateinit var data: EntityData
    private val unitActions = HashMap<EntityId, Action>()

    override fun initialize() {
        data = getSystem(DataSystem::class.java).getPhysicsData()
    }

    override fun terminate() {

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
            data.setComponent(id, ActionInfo(action.toString(), ActionStatus.STARTING))
        }
    }
}