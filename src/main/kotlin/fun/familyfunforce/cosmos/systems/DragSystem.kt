package `fun`.familyfunforce.cosmos.systems

import com.simsilica.es.EntitySet
import com.simsilica.mathd.Vec3d
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import `fun`.familyfunforce.cosmos.Drag
import `fun`.familyfunforce.cosmos.Velocity

class DragSystem: AbstractGameSystem() {
    private lateinit var dragEntities: EntitySet
    private lateinit var physSystem: LocalPhysicsSystem

    override fun initialize() {
        physSystem = getSystem(LocalPhysicsSystem::class.java)
        val data = getSystem(DataSystem::class.java).entityData
        dragEntities = data.getEntities(Velocity::class.java, Drag::class.java)
    }

    override fun update(time: SimTime) {
        dragEntities.applyChanges()
        dragEntities.forEach {
            val vel = it.get(Velocity::class.java).velocity
            val lenSqr = vel.lengthSq()
            //if velocity is near 0 skip
            val force: Vec3d = if(lenSqr <= EPSILON) {
                vel.mult(-1.0)
            } else{
                //force(drag) = (v^2)/2*dc
                val dc = it.get(Drag::class.java).dragCoefficient
                vel.normalize().mult(-lenSqr*0.5*dc)
            }
            physSystem.getPhysicsBody(it.id)?.applyForce(force)
        }
    }

    override fun terminate() {
        dragEntities.release()
    }
}