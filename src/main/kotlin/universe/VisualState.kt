package universe

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.asset.AssetManager
import com.jme3.asset.ModelKey
import com.jme3.material.Material
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.simsilica.es.Entity
import com.simsilica.es.EntityContainer
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.mathd.Vec3d

const val ID_KEY = "EID"

class VisualState: BaseAppState() {
    private val sceneNode = Node("Visual Scene")
    lateinit var am: AssetManager
    lateinit var debugMat: Material
    private lateinit var visContainer: VisualContainer
    var debug = true

    override fun initialize(_app: Application) {
        val app = _app as SpaceTraderApp
        am = app.assetManager
        debugMat = Material(am, "Common/MatDefs/Misc/Unshaded.j3md")
        visContainer = VisualContainer(app.manager.get(DataSystem::class.java).getPhysicsData())
        app.rootNode.attachChild(sceneNode)
    }

    override fun cleanup(app: Application?) {
        sceneNode.removeFromParent()
    }

    override fun onEnable() {
        visContainer.start()
    }

    override fun onDisable() {
        visContainer.stop()
    }

    override fun update(tpf: Float) {
        visContainer.update()
    }

    private inner class VisualContainer(data: EntityData): EntityContainer<VisObject>(data, Position::class.java,
        VisualAsset::class.java, Velocity::class.java) {

        override fun addObject(e: Entity): VisObject {
            val obj = VisObject(e.id, e.get(VisualAsset::class.java).asset, e.get(Position::class.java).position,
                e.get(Velocity::class.java).velocity)
            sceneNode.attachChild(obj.vis)
            return obj
        }

        override fun updateObject(vis: VisObject, e: Entity) {
            vis.update(e.get(Position::class.java).position, e.get(Velocity::class.java).velocity)
        }

        override fun removeObject(vis: VisObject, e: Entity) {
            vis.vis.removeFromParent()
        }
    }

    private inner class VisObject(_eid:EntityId, asset:String, position:Vec3d, velocity: Vec3d){
        val vis: Spatial = am.loadAsset(ModelKey(asset))!!
        init{
            if (debug) vis.setMaterial(debugMat)
            vis.setUserData(ID_KEY, _eid.id)
            vis.localTranslation = position.toVector3f()
            setVelocity(velocity)
        }

        /**
         * Setting Velocity is for directional only
         */
        fun setVelocity(vel:Vec3d){
            //do nothing if to slow, it should just stay at the last rotation
            if(vel.lengthSq() < EPSILON) return
            //very simply point in the direction
            vis.localRotation = vis.localRotation.lookAt(vel.toVector3f(), Vector3f.UNIT_Y)
        }

        fun update(position: Vec3d, velocity: Vec3d){
            vis.localTranslation = position.toVector3f()
            setVelocity(velocity)
        }
    }
}