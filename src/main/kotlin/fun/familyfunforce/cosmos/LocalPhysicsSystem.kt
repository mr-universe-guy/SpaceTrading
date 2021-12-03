package `fun`.familyfunforce.cosmos

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.app.state.BaseAppState
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.debug.Arrow
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
    private lateinit var app: SimpleApplication
    //debug stuff
    private var debugState = PhysicsDebugState()

    override fun initialize() {
        data = getSystem(DataSystem::class.java).getPhysicsData()
        //physBodies = data.getEntities(GridPosition::class.java, Velocity::class.java, Mass::class.java)
        physBodies = PhysicsBodyContainer(data)
        app = getSystem(SimpleApplication::class.java)
        //TODO:Turn debug off by default
        //setDebugView(false)
        app.stateManager.attach(debugState)
        physBodies.start()
    }

    override fun update(time: SimTime) {
        physBodies.physUpdate(time)
    }

    override fun terminate() {
        physBodies.stop()
    }

    fun setDebugView(debug:Boolean){
        debugState.isEnabled = debug
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

    /**
     * Private app state for displaying debug physics information
     */
    private inner class PhysicsDebugState: BaseAppState() {
        //map each eid to a DebugObject
        val debugMap = HashMap<EntityId, DebugObject>()
        //visual nodes and mats
        val debugNode = Node("Phys_Debug")
        lateinit var debugMat: Material
        //entity set
        lateinit var debugObjects: EntitySet

        override fun initialize(app: Application) {
            //load debug material
            debugMat = Material(app.assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
            debugMat.setColor("Color", ColorRGBA.Orange)
            //TODO: Remove forced camera from debug
            app.camera.location = Vector3f(0f,50f,0f)
            app.camera.lookAtDirection(Vector3f(0f,-1f,0f), Vector3f(0f,0f,1f))
        }

        override fun cleanup(app: Application) {

        }

        override fun onEnable() {
            //get entity set
            debugObjects = data.getEntities(Position::class.java, Velocity::class.java)
            app.enqueue {
                //once render thread is ready apply set changes and create all existing debug objects
                debugObjects.applyChanges()
                addAll(debugObjects)
                app.rootNode.attachChild(debugNode)
            }
        }

        override fun onDisable() {
            //release entity set
            debugObjects.release()
            app.enqueue {
                //remove scene
                debugNode.removeFromParent()
                //clear scene
                debugNode.detachAllChildren()
                //clear map
                debugMap.clear()
            }
        }

        override fun update(tpf: Float) {
            if(debugObjects.applyChanges()){
                removeAll(debugObjects.removedEntities)
                addAll(debugObjects.addedEntities)
                debugObjects.changedEntities.forEach {
                    val obj = debugMap[it.id]
                    obj?.update(it.get(Position::class.java).position, it.get(Velocity::class.java).velocity)
                }
            }
        }

        fun addAll(entities: Set<Entity>){
            entities.forEach {
                //create object and map it
                val obj = DebugObject(it.id.toString(), it.get(Position::class.java).position, it.get(Velocity::class.java).velocity)
                debugMap[it.id] = obj
                debugNode.attachChild(obj.spat)
            }
        }

        fun removeAll(entities: Set<Entity>){
            entities.forEach {
                //remove mapping, if mapping is not present throw an exception
                debugMap.remove(it.id)!!.spat.removeFromParent()
            }
        }

        private inner class DebugObject(id:String, position:Vec3d, velocity:Vec3d){
            //we use the arrow mesh to easily orient towards velocity vector with minimal code
            private val arrow = Arrow(velocity.toVector3f())
            val spat = Geometry(id, arrow)
            init{
                spat.material = debugMat
                spat.localTranslation = position.toVector3f()
            }

            fun update(pos:Vec3d, vel:Vec3d){
                spat.localTranslation = pos.toVector3f()
                arrow.setArrowExtent(vel.toVector3f())
            }
        }
    }
}