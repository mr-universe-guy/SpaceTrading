package universe

import com.simsilica.es.EntityData
import com.simsilica.es.EntitySet
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

/**
 * Local Physics System simply applies physics forces and updates velocities and positions
 */
class LocalPhysicsSystem: AbstractGameSystem() {
    private lateinit var physData: EntityData
    private lateinit var physBodies: EntitySet

    override fun initialize() {
        physData = getSystem(DataSystem::class.java).getPhysicsData()
        physBodies = physData.getEntities(GridPosition::class.java, Velocity::class.java, Mass::class.java)
    }

    override fun update(time: SimTime) {
        physBodies.applyChanges()
        //update physics positions
        physBodies.forEach {
            val pos = it.get(GridPosition::class.java).position
            val vel = it.get(Velocity::class.java).velocity
            it.set(GridPosition(pos.add(vel.mult(time.tpf))))
        }
    }

    override fun terminate() {
        physBodies.release()
    }
}