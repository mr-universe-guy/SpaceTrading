package `fun`.familyfunforce.cosmos

import com.simsilica.es.*
import com.simsilica.mathd.Vec3d
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

/**
 * A "close to zero" number to use for physics calculations.
 */
const val EPSILON = 0.01

/**
 * Local Physics System simply applies physics forces and updates velocities and positions
 */
class LocalPhysicsSystem: AbstractGameSystem() {
    private lateinit var data: EntityData
    private lateinit var physBodies: PhysicsBodyContainer

    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        //physBodies = data.getEntities(GridPosition::class.java, Velocity::class.java, Mass::class.java)
        physBodies = PhysicsBodyContainer(data)
        //app = getSystem(SimpleApplication::class.java)
        //TODO:Turn debug off by default
        //setDebugView(false)
        //app.stateManager.attach(debugState)
        physBodies.start()
    }

    override fun update(time: SimTime) {
        physBodies.physUpdate(time)
    }

    override fun terminate() {
        physBodies.stop()
    }

    fun getPhysicsBody(id:EntityId): PhysicsBody?{
        return physBodies.getObject(id)
    }

    class PhysicsBody(var position: Vec3d, var velocity: Vec3d, var mass: Double, val eid:EntityId){
        private val accumulator = Vec3d(0.0,0.0,0.0)

        /**
         * Applies the specified force multiplied by the
         */
        fun applyForce(force:Vec3d){
            accumulator.addLocal(force)
        }

        internal fun update(time: SimTime){
            //apply forces
            velocity = velocity.add(accumulator.divide(mass).mult(time.tpf))
            accumulator.set(0.0,0.0,0.0)
            //apply position
            position = position.add(velocity.mult(time.tpf))
        }
    }

    private class PhysicsBodyContainer(val data:EntityData) : EntityContainer<PhysicsBody>(data,
        Position::class.java, Velocity::class.java, Mass::class.java){
        override fun addObject(e: Entity): PhysicsBody {
            val initPos = e.get(Position::class.java).position
            val initVelocity = e.get(Velocity::class.java).velocity
            val initMass = e.get(Mass::class.java).mass
            return PhysicsBody(initPos, initVelocity, initMass, e.id)
        }

        override fun updateObject(`object`: PhysicsBody?, e: Entity?) {
            //do nothing
        }

        override fun removeObject(`object`: PhysicsBody?, e: Entity?) {
            //we shouldn't have to actually do anything here
        }

        fun physUpdate(time:SimTime){
            //This should only add and remove physics objects or update their mass
            update()
            //physics compute followed by publishing new physics data
            array.forEach {
                it.update(time)
                //update components
                data.setComponents(it.eid, Position(it.position), Velocity(it.velocity))
            }
        }
    }
}