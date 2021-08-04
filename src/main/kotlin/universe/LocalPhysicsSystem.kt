package universe

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.app.state.BaseAppState
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.debug.Arrow
import com.simsilica.es.Entity
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.EntitySet
import com.simsilica.mathd.Vec3d
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

/**
 * Local Physics System simply applies physics forces and updates velocities and positions
 */
class LocalPhysicsSystem: AbstractGameSystem() {
    private lateinit var data: EntityData
    private lateinit var physBodies: EntitySet
    private lateinit var app: SimpleApplication
    //debug stuff
    private var debugState = PhysicsDebugState()

    override fun initialize() {
        data = getSystem(DataSystem::class.java).getPhysicsData()
        physBodies = data.getEntities(GridPosition::class.java, Velocity::class.java, Mass::class.java)
        app = getSystem(SimpleApplication::class.java)
        app.stateManager.attach(PhysicsDebugState())
        setDebugView(true)
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

    fun setDebugView(debug:Boolean){
        debugState.isEnabled = debug
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
            debugObjects = data.getEntities(GridPosition::class.java, Velocity::class.java)
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
                    obj!!.update(it.get(GridPosition::class.java).position, it.get(Velocity::class.java).velocity)
                }
            }
        }

        fun addAll(entities: Set<Entity>){
            entities.forEach {
                //create object and map it
                val obj = DebugObject(it.id.toString(), it.get(GridPosition::class.java).position, it.get(Velocity::class.java).velocity)
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