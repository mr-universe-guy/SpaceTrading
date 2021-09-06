package universe

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.EntitySet
import com.simsilica.es.WatchedEntity
import com.simsilica.lemur.*
import com.simsilica.lemur.component.BorderLayout
import com.simsilica.lemur.component.BoxLayout
import com.simsilica.lemur.core.VersionedReference
import com.simsilica.lemur.input.FunctionId
import com.simsilica.lemur.input.InputMapper
import com.simsilica.lemur.input.InputState
import com.simsilica.lemur.input.StateFunctionListener
import com.simsilica.mathd.Vec3d

/**
 * A state to manage the interactive ship Hud and UI.
 * The ship hud consists of both lemur and jfx elements
 */
class ShipHudState: BaseAppState(), StateFunctionListener, LocalMapState.MapFocusListener {
    //lemur hud elements
    private val hudNode = Node("Hud_Gui")
    private val energyGauge= Label("XXX% Energy")
    private val velocityIndicator= Label("XXXX m/s")
    //all panels go on the dash
    private val dashboard = Container(BorderLayout())
    //mini map container
    private val mapContainer = Container(BorderLayout())
    private val mapInfoContainer = Container(BoxLayout(Axis.Y, FillMode.Even))
    //navigation panel
    private val navContainer = Container(BorderLayout())
    private lateinit var throttle : VersionedReference<Double>
    //
    private lateinit var mapper: InputMapper
    private lateinit var data: EntityData
    private lateinit var inRangeTargets: EntitySet
    private lateinit var actionSys: ActionSystem
//
    var playerId : EntityId? = null
    private var playerShip: WatchedEntity? = null
    private var target: WatchedEntity? = null
    private var mapSelection: EntityId? = null

    override fun initialize(_app: Application) {
        val app = _app as SpaceTraderApp
        data = app.manager.get(DataSystem::class.java).getPhysicsData()
        actionSys = app.manager.get(ActionSystem::class.java)
        //build lemur hud
        val screenHeight = app.camera.height
        val screenWidth = app.camera.width
        val readoutContainer = Container(BoxLayout(Axis.Y, FillMode.Even))
        readoutContainer.preferredSize = Vector3f(screenWidth.toFloat(), 100f, 0f)
        readoutContainer.localTranslation = Vector3f(0f, 100f, 0f)
        energyGauge.textHAlignment = HAlignment.Center
        readoutContainer.addChild(energyGauge)
        velocityIndicator.textHAlignment = HAlignment.Center
        readoutContainer.addChild(velocityIndicator)
        hudNode.attachChild(readoutContainer)
        //target info
        val targetPanel = Container(BorderLayout())
        //minimap info
        val mapInfoName = Label("")
        mapInfoName.name = "NAME"
        mapInfoContainer.addChild(mapInfoName)
        mapContainer.addChild(mapInfoContainer, BorderLayout.Position.North)
        //minimap
        val mapState = getState(LocalMapState::class.java)
        mapState.addFocusListener(this)
        val mapPanel = mapState.getMap()
        val mapSize = Vector3f(220f, 220f, 1f)
        mapContainer.addChild(mapPanel, BorderLayout.Position.Center)
        mapContainer.preferredSize = mapSize
        mapContainer.localTranslation = Vector3f(screenWidth-mapSize.x, mapSize.y, 0f)
        //hudNode.attachChild(mapContainer)
        //nav panel
        val throttleModel = DefaultRangedValueModel(0.0, 1.0, 1.0)
        throttle = throttleModel.createReference()
        val throttleSlider = Slider(throttleModel, Axis.Y)
        throttleSlider.incrementButton.removeFromParent()
        throttleSlider.decrementButton.removeFromParent()
        navContainer.addChild(throttleSlider, BorderLayout.Position.East)
        //nav options
        val navOptions = Container(BoxLayout(Axis.Y, FillMode.Even))
        val orbitButton = Button("Orbit")
        orbitButton.addClickCommands { orbitTarget() }
        navOptions.addChild(orbitButton)
        navContainer.addChild(navOptions, BorderLayout.Position.Center)
        //add all our panels to the single dashboard
        //let's go for screen width and .25 screen height
        val dashY = screenHeight*0.25f
        dashboard.preferredSize = Vector3f(screenWidth.toFloat(), dashY, 1f)
        dashboard.localTranslation = Vector3f(0f, dashY, 0f)
        dashboard.addChild(targetPanel, BorderLayout.Position.Center)
        dashboard.addChild(mapContainer, BorderLayout.Position.East)
        dashboard.addChild(navContainer, BorderLayout.Position.West)
        hudNode.attachChild(dashboard)
        //keyboard shortcuts
        mapper = GuiGlobals.getInstance().inputMapper
        mapper.addStateListener(this, SHIP_NEXT_TARGET)
        println("Ship Hud Enabled")
        inRangeTargets = data.getEntities(Position::class.java)
    }

    override fun cleanup(app: Application?) {
        //keyboard shortcuts
        mapper.removeStateListener(this, SHIP_NEXT_TARGET)
        getState(LocalMapState::class.java)?.removeFocusListener(this)
    }

    override fun onEnable() {
        val app = application as SpaceTraderApp
        app.guiNode.attachChild(hudNode)
        mapper.activateGroup(SHIP_INPUT_GROUP)
    }

    override fun onDisable() {
        hudNode.removeFromParent()
        mapper.deactivateGroup(SHIP_INPUT_GROUP)
    }

    override fun update(tpf: Float) {
        playerId?.let { watchPlayer(it) }
        if(playerShip != null){
            //do things with gui input
            if(throttle.update()) {
                val action = actionSys.getAction(playerShip?.id!!)
                action?.setThrottle(throttle.get())
            }
        }
        if(playerShip?.applyChanges() == true){
            updatePlayerGui(playerShip!!)
        }
        target?.applyChanges()
        inRangeTargets.applyChanges()
    }

    private fun orbitTarget(){

    }

    private fun updatePlayerGui(playerShip: WatchedEntity){
        val velocity = playerShip.get(Velocity::class.java)?.velocity ?: Vec3d(0.0,0.0,0.0)
        val floatFormat = "%.1f"
        velocityIndicator.text = "${floatFormat.format(velocity.length())} M/S"
        val energy = playerShip.get(Energy::class.java)?.curEnergy ?: 0.0
        energyGauge.text = "$energy% Energy"
    }

    private fun watchPlayer(id: EntityId){
        playerShip?.release()
        playerShip = data.watchEntity(id, Position::class.java, Energy::class.java, Velocity::class.java)
        playerId = null
    }

    /**
     * Target the next-furthest target until the furthest target has been found, then target the nearest
     */
    private fun nextTarget(){
        println("Searching for next furthest target")
        val pos = playerShip?.get(Position::class.java)?.position ?: return
        val farTgtDist = target?.get(Position::class.java)?.position?.distanceSq(pos) ?: 0.0
        var nearestDist = Double.MAX_VALUE
        var farTgtId: EntityId? = target?.id
        //find the next-furthest target from us currently
        inRangeTargets.forEach {
            if(it.id == playerShip?.id) return@forEach
            if(it.id == farTgtId) return@forEach
            val itDist = it.get(Position::class.java).position.distanceSq(pos)
            if(itDist > farTgtDist && itDist < nearestDist){
                nearestDist = itDist
                farTgtId = it.id
            }
        }
        if(farTgtId != target?.id){
            selectTarget(farTgtId!!)
        } else{
            nearestTarget()
        }
    }

    /**
     * Target the nearest target
     */
    private fun nearestTarget(){
        println("Searching for nearest target")
        val pos = playerShip?.get(Position::class.java)?.position ?: return
        var nearestDist = Double.MAX_VALUE
        var nearTgtId: EntityId? = target?.id
        inRangeTargets.forEach {
            if(it.id == playerShip?.id) return@forEach
            if(it.id == nearTgtId) return@forEach
            val itDist = it.get(Position::class.java).position.distanceSq(pos)
            if(itDist >= nearestDist) return@forEach
            nearestDist = itDist
            nearTgtId = it.id
        }
        nearTgtId?.let { selectTarget(it) }
    }

    /**
     * Set the active target to the given id
     */
    private fun selectTarget(targetId: EntityId?){
        target?.release()

        if(targetId == null){
            target = null
            println("Clearing target")
            return
        }
        target = data.watchEntity(targetId, Position::class.java, Name::class.java)
        val pos = data.getComponent(targetId, Position::class.java).position
        val name = data.getComponent(targetId, Name::class.java).name
        println("Targeting $name at $pos")
    }

    override fun iconFocused(id: EntityId?) {
        mapSelection = id
        val name:String = if(id == null) "" else data.getComponent(id, Name::class.java).name
        (mapInfoContainer.getChild("NAME") as Label).text = name
        println("Map item $id selected")
    }

    /**
     * Keyboard Input
     */
    override fun valueChanged(func: FunctionId?, value: InputState?, tpf: Double) {
        when(func){
            SHIP_NEXT_TARGET -> {
                if(InputState.Positive != value) return
                if(mapSelection == null){
                    nextTarget()
                } else{
                    selectTarget(mapSelection)
                }
            }
        }
    }
}