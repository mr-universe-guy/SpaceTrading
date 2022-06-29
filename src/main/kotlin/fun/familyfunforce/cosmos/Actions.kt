package `fun`.familyfunforce.cosmos

import com.jme3.network.serializing.Serializer
import com.jme3.network.serializing.serializers.EnumSerializer
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.mathd.Vec3d
import com.simsilica.sim.SimTime

object Actions{
    private val actions = arrayOf(
        MoveAction::class.java,
        OrbitAction::class.java,
        ApproachAction::class.java
    )

    fun serializeActions(){
        Serializer.registerClasses(*actions)
        Serializer.registerClass(ActionStatus::class.java, EnumSerializer())
    }
}
/**
 * Actions are per unit, short term actions such as Idle, Move, Follow, Orbit, Align, etc.
 */
@com.jme3.network.serializing.Serializable
enum class ActionStatus{
    STARTING,
    ONGOING,
    COMPLETE,
    FAILED
}

interface Action{
    fun update(id: EntityId, data: EntityData, time: SimTime): ActionStatus
    fun getStatus(): ActionStatus
    fun setStatus(_status: ActionStatus)
    fun getThrottle(): Double
    fun setThrottle(_throttle: Double)
}

abstract class AbstractAction: Action{
    private var throttle = 1.0
    override fun setThrottle(_throttle: Double) {throttle = _throttle}
    override fun getThrottle(): Double {return throttle}

    private var status = ActionStatus.STARTING
    override fun getStatus(): ActionStatus {return status}
    override fun setStatus(_status: ActionStatus) {status = _status}
}

/**
 * Steer the engine to maneuver the entity towards the specified destination
 */
@com.jme3.network.serializing.Serializable
class MoveAction(private var destination: Vec3d): AbstractAction() {
    constructor():this(Vec3d(0.0,0.0,0.0))
    override fun update(id: EntityId, data: EntityData, time: SimTime): ActionStatus {
        val pos = data.getComponent(id, Position::class.java).position
        val diff = destination.subtract(pos)
        if(diff.lengthSq() < 10){
            data.setComponents(id,
                EngineDriver(Vec3d(0.0,0.0,0.0)),
            )
            return ActionStatus.COMPLETE
        }
        data.setComponent(id, EngineDriver(diff.normalizeLocal().multLocal(getThrottle())))
        return ActionStatus.ONGOING
    }

    override fun toString(): String {
        return "Move To $destination"
    }
}

@com.jme3.network.serializing.Serializable
class ApproachAction(var targetId: EntityId, var distance:Double): AbstractAction(){
    constructor():this(EntityId.NULL_ID, 0.0)
    override fun update(id: EntityId, data: EntityData, time: SimTime): ActionStatus {
        val pos = data.getComponent(id, Position::class.java).position
        val tgtPos = data.getComponent(targetId, Position::class.java)?.position ?: return ActionStatus.FAILED
        val diff = tgtPos.subtract(pos)
        if(diff.lengthSq() < distance){
            data.setComponent(id, EngineDriver(Vec3d(0.0,0.0,0.0)))
            return ActionStatus.COMPLETE
        }
        data.setComponent(id, EngineDriver(diff.normalizeLocal().multLocal(getThrottle())))
        return ActionStatus.ONGOING
    }

}

@com.jme3.network.serializing.Serializable
class OrbitAction(var targetId: EntityId, var distance: Double): AbstractAction(){
    constructor():this(EntityId.NULL_ID, 0.0)
    override fun update(id: EntityId, data: EntityData, time: SimTime): ActionStatus {
        //if target or self are missing a position fail early
        val pos = data.getComponent(id, Position::class.java)?.position ?: return ActionStatus.FAILED
        val vel = data.getComponent(id, Velocity::class.java)?.velocity ?: return ActionStatus.FAILED
        val tgtPos = data.getComponent(targetId, Position::class.java)?.position ?: return ActionStatus.FAILED
        //get math local to target, add velocity to compensate!
        val localPos = pos.subtract(tgtPos).add(vel)
        val dist = localPos.length()
        val localDir = localPos.mult(1.0/dist)
        //we can use our current position and velocity as an "up" dir to allow non-planar orbits at the cost of another sqrt
        var upDir = localDir.cross(vel.normalize())
        if(upDir.isNaN) upDir = Vec3d.UNIT_Y
        //val upDir = Vec3d.UNIT_Y
        //normal between current pos and up is the orbit's tangent
        val orbitDir = upDir.cross(localDir)
        //add the error of the distance to our orbit direction and normalize
        val steer = orbitDir.addLocal(localDir.mult(distance-dist)).normalizeLocal()
        //TODO: Incorporate a throttle control
        data.setComponent(id, EngineDriver(steer.mult(getThrottle())))
        //println(dist)
        return ActionStatus.ONGOING
    }
}