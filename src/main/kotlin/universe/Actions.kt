package universe

import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.mathd.Vec3d
import com.simsilica.sim.SimTime

/**
 * Actions are per unit, short term actions such as Idle, Move, Follow, Orbit, Align, etc.
 */

enum class ActionStatus{
    STARTING,
    ONGOING,
    COMPLETE,
    FAILED
}

interface Action{
    fun update(id: EntityId, data: EntityData, time: SimTime): ActionStatus
    fun getStatus(): ActionStatus
    fun setStatus(status: ActionStatus)
}

abstract class AbstractAction: Action{
    private var status = ActionStatus.STARTING

    override fun getStatus(): ActionStatus {
        return status
    }

    override fun setStatus(_status: ActionStatus) {
        status = _status
    }
}

/**
 * Steer the engine to maneuver the entity towards the specified destination
 */
class MoveAction(private val destination: Vec3d): AbstractAction() {
    override fun update(id: EntityId, data: EntityData, time: SimTime): ActionStatus {
        //TODO: Take into account engine strength and braking distance
        val pos = data.getComponent(id, Position::class.java).position
        val diff = destination.subtract(pos)
        //println("$id heading towards $pos, ${diff.length()}")
        if(diff.lengthSq() < 10){
            data.setComponents(id,
                EngineDriver(Vec3d(0.0,0.0,0.0)),
            )
            return ActionStatus.COMPLETE
        }
        data.setComponent(id, EngineDriver(diff.normalizeLocal()))
        return ActionStatus.ONGOING
    }

    override fun toString(): String {
        return "Move To $destination"
    }
}