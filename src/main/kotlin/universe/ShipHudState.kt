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
import com.simsilica.lemur.component.BoxLayout
import com.simsilica.lemur.input.FunctionId
import com.simsilica.lemur.input.InputMapper
import com.simsilica.lemur.input.InputState
import com.simsilica.lemur.input.StateFunctionListener
import com.simsilica.mathd.Vec3d

/**
 * A state to manage the interactive ship Hud and UI.
 * The ship hud consists of both lemur and jfx elements
 */
class ShipHudState: BaseAppState(), StateFunctionListener {
    //lemur hud elements
    private val hudNode = Node("Hud_Gui")
    private lateinit var energyGauge: Label
    private lateinit var velocityIndicator: Label
    //
    private lateinit var mapper: InputMapper
    private lateinit var data: EntityData
    private lateinit var inRangeTargets: EntitySet

    var playerId : EntityId? = null
    private var playerShip: WatchedEntity? = null
    private var target: WatchedEntity? = null
    override fun initialize(_app: Application) {
        val app = _app as SpaceTraderApp
        //build lemur hud
        val screenWidth = app.camera.width
        val readoutContainer = Container(BoxLayout(Axis.Y, FillMode.Even))
        readoutContainer.preferredSize = Vector3f(screenWidth.toFloat(), 100f, 0f)
        readoutContainer.localTranslation = Vector3f(0f, 100f, 0f)
        energyGauge = Label("XXX% Energy")
        energyGauge.textHAlignment = HAlignment.Center
        readoutContainer.addChild(energyGauge)
        velocityIndicator = Label("XXXX m/s")
        velocityIndicator.textHAlignment = HAlignment.Center
        readoutContainer.addChild(velocityIndicator)
        hudNode.attachChild(readoutContainer)
        //minimap
        val mapPanel = getState(LocalMapState::class.java).getMap()
        val mapSize = Vector3f(220f, 220f, 1f)
        mapPanel.preferredSize = mapSize
        mapPanel.localTranslation = Vector3f(screenWidth-mapSize.x, mapSize.y, 0f)
        hudNode.attachChild(mapPanel)
        //keyboard shortcuts
        mapper = GuiGlobals.getInstance().inputMapper
        mapper.addStateListener(this, SHIP_NEXT_TARGET)
        println("Ship Hud Enabled")
        data = app.manager.get(DataSystem::class.java).getPhysicsData()
        inRangeTargets = data.getEntities(Position::class.java)
    }

    override fun cleanup(app: Application?) {
        //keyboard shortcuts
        mapper.removeStateListener(this, SHIP_NEXT_TARGET)
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
        if(playerShip?.applyChanges() == true){
            updatePlayerGui(playerShip!!)
        }
        target?.applyChanges()
        inRangeTargets.applyChanges()
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
    private fun selectTarget(targetId: EntityId){
        target?.release()
        target = data.watchEntity(targetId, Position::class.java, Name::class.java)
        val name = data.getComponent(targetId, Name::class.java).name
        val pos = data.getComponent(targetId, Position::class.java).position
        println("Targeting $name at $pos")
    }

    override fun valueChanged(func: FunctionId?, value: InputState?, tpf: Double) {
        when(func){
            SHIP_NEXT_TARGET -> {
                if(InputState.Positive != value) return
                println("Select next target")
                nextTarget()
            }
        }
    }
}