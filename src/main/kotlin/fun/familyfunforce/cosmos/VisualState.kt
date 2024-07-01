package `fun`.familyfunforce.cosmos

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.asset.AssetManager
import com.jme3.asset.ModelKey
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import com.jme3.scene.shape.Cylinder
import com.jme3.scene.shape.Sphere
import com.simsilica.es.Entity
import com.simsilica.es.EntityContainer
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.EntitySet
import com.simsilica.lemur.anim.AnimationState
import com.simsilica.lemur.anim.Tweens
import com.simsilica.mathd.Vec3d
import java.lang.System

const val ID_KEY = "EID"

/**
 * Loads visual data for spatials
 */
class VisualState: BaseAppState() {
    private val sceneNode = Node("Visual Scene")
    private lateinit var animationState: AnimationState
    lateinit var am: AssetManager
    lateinit var debugMat: Material
    private lateinit var visContainer: VisualContainer
    private lateinit var weaponAttacks: EntitySet
    var debug = true
    private var lastUpdateTime = Long.MIN_VALUE
    private var deltaTime=1f

    override fun initialize(_app: Application) {
        val app = _app as SpaceTraderApp
        animationState = getState(AnimationState::class.java)
        am = app.assetManager
        val data = getState(ClientDataState::class.java).entityData
        debugMat = Material(am, "Common/MatDefs/Misc/Unshaded.j3md")
        visContainer = VisualContainer(data)
        weaponAttacks = data.getEntities(
            VisualAsset::class.java,
            Attack::class.java,
            Parent::class.java,
            TargetId::class.java
        )
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
        if(visContainer.update()) {
            val frameTime = System.nanoTime()
            //get the delta from the last received frame to now in seconds
            deltaTime = ((frameTime - lastUpdateTime) * NANOS_TO_SECONDS).toFloat()
            //use the delta time to interpolate from the previous position to their new positions
            lastUpdateTime = frameTime
        }
        if(weaponAttacks.applyChanges()){
            weaponAttacks.addedEntities.forEach {
                //TODO spawn effect from parent to target
                val asset = it.get(VisualAsset::class.java).asset
                val parentId = it.get(Parent::class.java).parentId
                val targetId = it.get(TargetId::class.java).targetId
                //get locations
                val parentSpatial = getSpatialFromId(parentId)
                val targetSpatial = getSpatialFromId(targetId)
                if(parentSpatial == null || targetSpatial == null){
                    return@forEach
                }
                val parentPos = parentSpatial.localTranslation
                val targetPos = targetSpatial.localTranslation
                //get weapon type to draw
                if("Laser" == asset){
                    val laserDuration = 0.5
                    //draw a cylinder from parent to target for now
                    val length = targetPos.distance(parentPos).coerceAtLeast(0.1f)
                    val mesh = Cylinder(8,8,1f,length)
                    val geo = Geometry("Attack@${it.id.id}", mesh)
                    val mat = debugMat.clone()
                    //tween to change colors and erase spatial
                    val color = ColorRGBA()
                    val colorTween = ColorTweener(ColorRGBA.White, ColorRGBA.Red, color, laserDuration)
                    mat.setColor("Color", color)
                    geo.material = mat
                    geo.addControl(HalfwayBetweenControl(parentSpatial, targetSpatial))
                    val removeTween = Tweens.callMethod(geo, "removeFromParent")
                    animationState.add(colorTween, removeTween)
                    sceneNode.attachChild(geo)
                }
            }
        }
    }

    fun getSpatialFromId(id:EntityId): Spatial?{
        return visContainer.getObject(id)?.vis
    }

    private inner class VisualContainer(data: EntityData): EntityContainer<VisObject>(
        data,
        Position::class.java,
        VisualAsset::class.java,
        Velocity::class.java
    ) {

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

    /**
     * Control that interpolates location between 2 frames based on the duration
     */
    private inner class VisControl(private var lastPos:Vector3f, _nextPos:Vector3f): AbstractControl() {
        /**
         * The position to interpolate towards
         */
        var nextPos = _nextPos
            set(value) {lastPos=Vector3f(spatial.localTranslation); curTime=0f; field=value}
        var curTime = 0f

        /**
         * The amount of time to interpolate positions. Ideally this will be the amount of time between the last frame
         * and the NEXT frame which still hasn't been delivered yet. Better to simply use the last frame time and hope it's stable.
         */

        override fun controlUpdate(tpf: Float) {
            curTime+=tpf
            spatial.localTranslation = FastMath.interpolateLinear(curTime/deltaTime, lastPos,nextPos)
        }

        override fun controlRender(rm: RenderManager?, vp: ViewPort?) {}
    }

    private inner class VisObject(eid:EntityId, asset:String, position:Vec3d, velocity: Vec3d){
        var visualizer:VisControl
        val vis: Spatial
        init{
            //Use a switch statement for now to implement super simple debug stuff
            when(asset){
                "ASTEROID"->{
                    val mesh = Sphere(8,8,5f)
                    vis = Geometry("Asteroid", mesh)
                    println("Asteroid created")
                }
                else -> {
                    vis = am.loadAsset(ModelKey(asset))!!
                }
            }
            if (debug) vis.setMaterial(debugMat)
            vis.setUserData(ID_KEY, eid.id)
            val renderPos = position.toVector3f()
            vis.localTranslation = renderPos
            setVelocity(velocity)
            visualizer = VisControl(renderPos, renderPos)
            vis.addControl(visualizer)
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

        /**
         * apply transforms to visual objects
         */
        fun update(nextPos:Vec3d, velocity: Vec3d){
            visualizer.nextPos=nextPos.toVector3f()
            setVelocity(velocity)
        }
    }

    private inner class HalfwayBetweenControl(val a:Spatial, val b:Spatial): AbstractControl(){
        override fun controlUpdate(tpf: Float) {
            spatial.localTranslation = a.localTranslation.add(b.localTranslation).multLocal(0.5f)
            spatial.lookAt(b.localTranslation, Vector3f.UNIT_Y)
        }

        override fun controlRender(rm: RenderManager?, vp: ViewPort?) {}
    }
}