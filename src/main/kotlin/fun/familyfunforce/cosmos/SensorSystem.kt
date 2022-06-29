package `fun`.familyfunforce.cosmos

import com.simsilica.es.*
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

class SensorSystem: AbstractGameSystem() {
    private lateinit var data: EntityData
    private lateinit var sensors: EntitySet

    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        sensors = data.getEntities(Position::class.java, Sensors::class.java, TargetLock::class.java)
    }

    override fun terminate() {
        sensors.release()
    }

    override fun update(time: SimTime?) {
        sensors.applyChanges()
        sensors.forEach {
            val targetId = it.get(TargetLock::class.java).targetId
            //check if target exists
            val tgtPos = data.getComponent(targetId, Position::class.java)?.position ?: return@forEach
            val pos = it.get(Position::class.java).position
            val sensorRange = it.get(Sensors::class.java).range
            //check if target is within range
            if(tgtPos.distanceSq(pos) <= sensorRange*sensorRange) return@forEach
            //target not in range, break lock
            data.removeComponent(it.id, TargetLock::class.java)
        }
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
        data.setComponent(sensorId, TargetLock(targetId))
        return true
    }
}