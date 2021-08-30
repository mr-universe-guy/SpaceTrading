package universe

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.input.event.MouseButtonEvent
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector2f
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.simsilica.es.*
import com.simsilica.lemur.Panel
import com.simsilica.lemur.component.IconComponent
import com.simsilica.lemur.component.QuadBackgroundComponent
import com.simsilica.lemur.core.VersionedReference
import com.simsilica.lemur.event.DefaultMouseListener
import com.simsilica.lemur.event.MouseAppState
import com.simsilica.lemur.event.MouseEventControl
import com.simsilica.mathd.Vec3d

private const val CATEGORY_KEY = "Category"
/**
 * A map showing all entities in the local space as simple symbols and markers, good for a minimap or battle map
 */
class LocalMapState: BaseAppState() {

    private val mapNode = Node("Battle_Map")
    private var mapCam: Camera = Camera(0,0)
    private lateinit var mapPanel: VersionedReference<Panel>
    private lateinit var mapViewport: ViewPort
    private lateinit var data: EntityData
    private lateinit var mapObjects: MapObjectContainer
    private var mapRadius = 100f
    private var followTarget: WatchedEntity? = null
    var targetId: EntityId? = null

    override fun initialize(_app: Application) {
        val app = _app as SpaceTraderApp
        //test input
        //initialize camera as top down
        mapNode.cullHint = Spatial.CullHint.Never
        mapNode.queueBucket = RenderQueue.Bucket.Transparent
        mapCam = Camera(app.camera.width, app.camera.height)
        mapCam.isParallelProjection = true
        //I think we may have to transform the camera for this to work
        mapViewport = app.renderManager.createPostView("Map_Viewport", mapCam)
        mapViewport.setClearFlags(false, true, true)
        mapViewport.backgroundColor = ColorRGBA.BlackNoAlpha
        mapViewport.attachScene(mapNode)
        //add scene and viewport to pick state
        val mouseState = app.stateManager.getState(MouseAppState::class.java)
        mouseState.addCollisionRoot(mapNode, mapViewport)
        //map panel
        val panel = VersionedPanel()
        panel.background = QuadBackgroundComponent(ColorRGBA(0f,0.1f,0f,0.5f))
        mapPanel = panel.createReference()
        //default map stuff (grid and range bands)

        //entities
        data = app.manager.get(DataSystem::class.java).getPhysicsData()
        mapObjects = MapObjectContainer(data)
    }

    override fun cleanup(app: Application) {
        app.renderManager.removeMainView(mapViewport)
        app.stateManager.getState(MouseAppState::class.java).removeCollisionRoot(mapViewport)
    }

    override fun onEnable() {
        mapObjects.start()
    }

    override fun onDisable() {
        mapObjects.stop()
    }

    //This is only for 1:1 aspect panels, enforce this for the minimap, so we can have easy math for now
    private fun viewportToPanel(){
        val panel = mapPanel.get()
        val size = panel.preferredSize
        val pos = panel.localTranslation
        //cam
        val camWidth = mapCam.width
        val camHeight = mapCam.height
        //set viewport
        val x1 = pos.x
        val x2 = pos.x+size.x
        val y1 = pos.y-size.y
        val y2 = pos.y
        val aspect = size.y/size.x
        mapCam.setViewPort(x1/camWidth, x2/camWidth, y1/camHeight, y2/camHeight)
        val mapHeight = mapRadius*aspect
        mapCam.setFrustum(-10f,10f,mapRadius, -mapRadius, mapHeight, -mapHeight)
    }

    override fun update(tpf: Float) {
        if(targetId != null){
            followTarget?.release()
            followTarget = data.watchEntity(targetId, Position::class.java)
            targetId = null
        }
        if(followTarget?.applyChanges() == true){
            val pos = followTarget!!.get(Position::class.java).position
            mapNode.localTranslation = Vector3f(pos.x.toFloat(), -pos.z.toFloat(), -9f)
        }
        mapObjects.update()
        if(mapPanel.update()) {
            viewportToPanel()
        }
        mapNode.updateLogicalState(tpf)
        mapNode.updateGeometricState()
    }

    fun getMapPanel(): Panel{
        return mapPanel.get()
    }

    private inner class MapObject(val id: EntityId, pos:Vec3d, category: Category){
        val icon = Panel()

        init{
            icon.setUserData(ID_KEY, id.id)
            icon.preferredSize = Vector3f(5f,5f,1f)
            setIconImage(category)
            setPosition(pos)
            //clicking
            icon.addControl(MouseEventControl(IconMouseHandler(this)))
        }

        fun setIconImage(category: Category){
            val path = when(category){
                Category.SHIP -> "UI/starfighter.png"
                Category.ASTEROID -> "UI/asteroid.png"
            }
            val component = IconComponent(path)
            component.iconSize = Vector2f(10f, 10f)
            icon.background = component
            icon.setUserData(CATEGORY_KEY, category.ordinal)
        }

        fun setPosition(pos: Vec3d){
            icon.localTranslation = Vector3f(-pos.x.toFloat(),pos.z.toFloat(), 0f)
        }
    }

    private inner class MapObjectContainer(_data: EntityData): EntityContainer<MapObject>(_data, Position::class.java,
        ObjectCategory::class.java){

        override fun addObject(e: Entity): MapObject {
            val id = e.id
            val pos = e.get(Position::class.java).position
            val category = e.get(ObjectCategory::class.java).category
            val obj = MapObject(id, pos, category)
            mapNode.attachChild(obj.icon)
            return obj
        }

        override fun updateObject(obj: MapObject, e: Entity) {
            val pos = e.get(Position::class.java).position
            obj.setPosition(pos)
            val cat = e.get(ObjectCategory::class.java).category
            if(cat.ordinal != obj.icon.getUserData(CATEGORY_KEY)) obj.setIconImage(cat)
        }

        override fun removeObject(obj: MapObject, e: Entity?) {
            obj.icon.removeFromParent()
        }
    }

    private class IconMouseHandler(val obj: MapObject): DefaultMouseListener(){
        override fun mouseButtonEvent(event: MouseButtonEvent?, target: Spatial?, capture: Spatial?) {
            if(event?.isPressed == false) return
            event?.setConsumed()
            println("Clicked icon ${obj.id}")
        }
    }
}