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
import com.simsilica.lemur.*
import com.simsilica.lemur.component.BorderLayout
import com.simsilica.lemur.component.BoxLayout
import com.simsilica.lemur.component.IconComponent
import com.simsilica.lemur.component.QuadBackgroundComponent
import com.simsilica.lemur.core.GuiControl
import com.simsilica.lemur.core.VersionedReference
import com.simsilica.lemur.event.DefaultMouseListener
import com.simsilica.lemur.event.FocusMouseListener
import com.simsilica.lemur.event.MouseAppState
import com.simsilica.lemur.event.MouseEventControl
import com.simsilica.lemur.focus.FocusChangeEvent
import com.simsilica.lemur.focus.FocusChangeListener
import com.simsilica.lemur.focus.FocusManagerState
import com.simsilica.mathd.Vec3d

private const val CATEGORY_KEY = "Category"
const val MAP_LAYER = "Map"
/**
 * A map showing all entities in the local space as simple symbols and markers, good for a minimap or battle map
 */
class LocalMapState: BaseAppState() {

    private val mapNode = Node("Battle_Map")
    private val mapOffset = Node("Map_Offset")
    private var mapCam: Camera = Camera(0,0)
    private val mapContainer = Container(BorderLayout())
    private lateinit var mapPanel: VersionedReference<Panel>
    private lateinit var mapViewport: ViewPort
    private lateinit var data: EntityData
    private lateinit var mapObjects: MapObjectContainer
    private var mapRadius = 100f
    private var mapFocus: WatchedEntity? = null
    private var followTarget: WatchedEntity? = null
    var targetId: EntityId? = null

    override fun initialize(_app: Application) {
        val app = _app as SpaceTraderApp
        //test input
        //initialize camera as top down
        mapNode.cullHint = Spatial.CullHint.Never
        mapNode.queueBucket = RenderQueue.Bucket.Transparent
        mapNode.attachChild(mapOffset)
        mapCam = Camera(app.camera.width, app.camera.height)
        mapCam.isParallelProjection = true
        //I think we may have to transform the camera for this to work
        mapViewport = app.renderManager.createPostView("Map_Viewport", mapCam)
        mapViewport.setClearFlags(false, true, true)
        mapViewport.backgroundColor = ColorRGBA.BlackNoAlpha
        mapViewport.attachScene(mapNode)
        //add scene and viewport to pick state, push map in front of gui for picking
        val mouseState = app.stateManager.getState(MouseAppState::class.java)
        mouseState.addCollisionRoot(mapNode, mapViewport, MAP_LAYER)
        mouseState.setPickLayerOrder(MAP_LAYER, MouseAppState.PICK_LAYER_GUI, MouseAppState.PICK_LAYER_SCENE)
        //println(mouseState.pickLayerOrder.contentToString())
        //map panel
        val panel = VersionedPanel()
        panel.addControl(MouseEventControl(MapPanelHandler()))
        panel.background = QuadBackgroundComponent(ColorRGBA(0f,0.1f,0f,0.5f))
        panel.localTranslation = Vector3f(0f,0f,10f)
        mapPanel = panel.createReference()
        mapContainer.addChild(mapPanel.get(), BorderLayout.Position.Center)
        //default map stuff (grid and range bands)
        val focusContainer = Container(BoxLayout(Axis.Y, FillMode.Even))
        val nameLabel = Label("Name")
        nameLabel.name = "Name"
        focusContainer.addChild(nameLabel)
        val distanceLabel = Label("Distance")
        distanceLabel.name = "Distance"
        focusContainer.addChild(distanceLabel)
        mapContainer.addChild(focusContainer, BorderLayout.Position.North)
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

    private fun viewportToPanel(){
        val panel = mapPanel.get()
        val size = panel.size
        val pos = panel.worldTranslation
        //cam
        val camWidth = mapCam.width
        val camHeight = mapCam.height
        //set viewport
        val aspect = size.y/size.x
        mapCam.setViewPort(pos.x/camWidth, (pos.x+size.x)/camWidth, (pos.y-size.y)/camHeight, pos.y/camHeight)
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
            mapOffset.localTranslation = Vector3f(pos.x.toFloat(), -pos.z.toFloat(), 9f)
        }
        mapFocus?.applyChanges()
        mapFocus?.let { updateFocusedInfo() }
        mapObjects.update()
        if(mapPanel.update()) {
            viewportToPanel()
        }
        mapNode.updateLogicalState(tpf)
        mapNode.updateGeometricState()
    }

    private fun updateFocusedInfo(){
        //update focus info above minimap
        val name = mapFocus?.get(Name::class.java)?.name ?: ""
        val followPos = followTarget?.get(Position::class.java)?.position
        val distance = mapFocus?.get(Position::class.java)?.position?.distance(followPos)
        val nl = mapContainer.getChild("Name") as Label?
        val dl = mapContainer.getChild("Distance") as Label?
        nl?.text = name
        if(distance == null) {
            dl?.text = ""
        } else{
            dl?.text = "%.2f m".format(distance)
        }
    }

    fun getMap(): Panel{
        return mapContainer
    }

    private inner class MapObject(val id: EntityId, pos:Vec3d, category: Category){
        val icon = Panel()

        init{
            icon.setUserData(ID_KEY, id.id)
            icon.preferredSize = Vector3f(5f,5f,1f)
            setIconImage(category)
            setPosition(pos)
            //clicking
            icon.addControl(MouseEventControl(FocusMouseListener.INSTANCE))
            icon.getControl(GuiControl::class.java).addFocusChangeListener(IconFocusListener(this))
            icon.setUserData(CATEGORY_KEY, category.ordinal)
        }

        fun setIconImage(category: Category){
            val path = when(category){
                Category.SHIP -> "UI/starfighter.png"
                Category.ASTEROID -> "UI/asteroid.png"
            }
            val component = IconComponent(path)
            component.iconSize = Vector2f(10f, 10f)
            component.color = ColorRGBA.Gray
            icon.background = component
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
            mapOffset.attachChild(obj.icon)
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

    private inner class IconFocusListener(val target: MapObject): FocusChangeListener{
        val bg = target.icon.background as IconComponent

        override fun focusGained(event: FocusChangeEvent?) {
            bg.color = ColorRGBA.Yellow
            mapFocus?.release()
            mapFocus = data.watchEntity(target.id, Name::class.java, Position::class.java)
            //for some reason the first frame of watching an entity doesn't return as updated... let's fix that
            mapFocus?.applyChanges()
            updateFocusedInfo()
        }

        override fun focusLost(event: FocusChangeEvent?) {
            bg.color = ColorRGBA.Gray
            mapFocus?.release()
            mapFocus = null
            updateFocusedInfo()
        }
    }

    private inner class MapPanelHandler: DefaultMouseListener(){
        override fun mouseButtonEvent(event: MouseButtonEvent?, target: Spatial?, capture: Spatial?) {
            if(event?.isPressed == false) return
            event?.setConsumed()
            //un-focus whatever is focused
            getState(FocusManagerState::class.java).focus = null
        }
    }
}