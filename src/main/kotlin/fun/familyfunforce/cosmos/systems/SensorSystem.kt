package `fun`.familyfunforce.cosmos.systems

import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.EntitySet
import com.simsilica.mathd.Vec3d
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import `fun`.familyfunforce.cosmos.*
import kotlin.math.acos

class SensorSystem: AbstractGameSystem() {
    private lateinit var data: EntityData
    private lateinit var sensors: EntitySet

    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        sensors = data.getEntities(
            Position::class.java,
            Sensors::class.java,
            TargetId::class.java,
            Velocity::class.java,
//            Parent::class.java
        )
    }

    override fun terminate() {
        sensors.release()
    }

    override fun update(time: SimTime?) {
        sensors.applyChanges()
        sensors.forEach {
            val targetId = it.get(TargetId::class.java).targetId
            //check if target exists
            val tgtPos = data.getComponent(targetId, Position::class.java)?.position
            if(tgtPos == null){
                breakLock(it.id)
                return@forEach
            }
            val pos = it.get(Position::class.java).position
            val sensorRange = it.get(Sensors::class.java).range
            val distance = tgtPos.distanceSq(pos)
            //check if target is within range
            if(distance > sensorRange*sensorRange) {
                //target not in range, break lock
                breakLock(it.id)
                return@forEach
            }
            //we're good, get tracking info
            //angular velocity is the angle between the tgt position and the tgtposition+difference in velocity
            val tgtVel = data.getComponent(targetId, Velocity::class.java)?.velocity ?: Vec3d(0.0,0.0,0.0)
            val vel = it.get(Velocity::class.java).velocity
            val posOffA = tgtPos.subtract(pos)
            val posOffB = posOffA.add(vel.subtract(tgtVel))
            //acos(DOT(A,B)/len(A)*len(B)) = angle between 2 3d vectors
            val angV = acos(posOffB.dot(posOffA)/(posOffA.length()*posOffB.length()))
            //valid track, fill in info
            it.set(TargetTrack(distance, angV))
        }
    }

    private fun breakLock(sensorId: EntityId){
        data.removeComponent(sensorId, TargetTrack::class.java)
        data.removeComponent(sensorId, TargetId::class.java)
    }

    fun acquireLock(sensorId: EntityId, targetId: EntityId) : Boolean{
        //get target position, missing position = dead (for now)
        val tgtPos = data.getComponent(targetId, Position::class.java)?.position ?: return false
        //now check sensor position, same deal
        val pos = data.getComponent(sensorId, Position::class.java)?.position ?: return false
        //continue for sensor stuff
        val sensorRange = data.getComponent(sensorId, Sensors::class.java)?.range ?: return false
        if(tgtPos.distanceSq(pos) > sensorRange*sensorRange) return false
        //we made it, establish the target lock
        data.setComponent(sensorId, TargetId(targetId))
        println("$sensorId has locked onto target $targetId")
        return true
    }
}