package universe

import com.simsilica.es.EntityData
import com.simsilica.es.EntitySet
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import kotlin.math.sqrt

/**
 * Steering System handles all steering and thrust related physics forces
 */
class EngineSystem: AbstractGameSystem() {
    private lateinit var data:EntityData
    private lateinit var engines:EntitySet
    private lateinit var physicsSystem: LocalPhysicsSystem

    override fun initialize() {
        data = getSystem(DataSystem::class.java).getPhysicsData()
        engines = data.getEntities(Velocity::class.java, Engine::class.java, EngineDriver::class.java)
        physicsSystem = getSystem(LocalPhysicsSystem::class.java)
    }

    override fun update(time: SimTime?) {
        engines.applyChanges()
        engines.forEach {
            val engine = it.get(Engine::class.java)
            //apply full thrust until the percentage of total speed is met in the chosen direction
            val targetVelocity = it.get(EngineDriver::class.java).direction.mult(engine.maxSpeed)
            //what direction to apply the thrust
            val thrustDiff = targetVelocity.subtract(it.get(Velocity::class.java).velocity)
            //if our desired velocity is equal to our current velocity do nothing
            val diffMag = thrustDiff.lengthSq()
            if(diffMag <= EPSILON) return
            //apply thrust up to the maximum thrust value towards target vel
            val multiplier = if(diffMag > engine.thrust*engine.thrust) engine.thrust else sqrt(diffMag)
            val thrust = thrustDiff.normalize().mult(multiplier)
            physicsSystem.getPhysicsBody(it.id)!!.applyForce(thrust)
        }
    }

    override fun terminate() {
        engines.release()
    }
}