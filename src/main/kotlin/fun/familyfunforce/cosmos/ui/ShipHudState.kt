package `fun`.familyfunforce.cosmos.ui

import `fun`.familyfunforce.cosmos.*
import `fun`.familyfunforce.cosmos.Name
import `fun`.familyfunforce.cosmos.event.ApproachOrderEvent
import `fun`.familyfunforce.cosmos.event.OrbitOrderEvent
import `fun`.familyfunforce.cosmos.event.ThrottleOrderEvent
import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Node
import com.jme3.scene.control.AbstractControl
import com.simsilica.es.*
import com.simsilica.event.EventBus
import com.simsilica.lemur.*
import com.simsilica.lemur.component.BorderLayout
import com.simsilica.lemur.component.BoxLayout
import com.simsilica.lemur.core.VersionedReference
import com.simsilica.lemur.event.PopupState
import com.simsilica.lemur.input.FunctionId
import com.simsilica.lemur.input.InputMapper
import com.simsilica.lemur.input.InputState
import com.simsilica.lemur.input.StateFunctionListener
import com.simsilica.lemur.style.ElementId
import com.simsilica.mathd.Vec3d
import kotlin.math.max

/**
 * A state to manage the interactive ship Hud and UI.
 * The ship hud consists of both lemur and jfx elements
 */
class ShipHudState: BaseAppState(), StateFunctionListener{
    private lateinit var targetMap: TargetContainer
    private lateinit var shipEquipment: EquipmentContainer
    private data class TargetUIElement(val id:EntityId, val uiPane:HudBracket, var pos:Position)
    private data class EquipmentUIElement(val id:EntityId, val equipPane:Container, val activeButton:Checkbox,
                                          val cycleProg:ProgressBar, var cycleEnd:Long)

    //lemur hud elements
    private val hudNode = Node("Hud_Gui")
    private val energyGauge= Label("XXX% Energy")
    private val velocityIndicator= Label("XXXX m/s")
    //all panels go on the dash
    private val dashboard = Container(BorderLayout())
    //interaction panel
    private val equipmentPanel = Container(BoxLayout(Axis.X, FillMode.None))
    //mini map container
    private val mapContainer = Container(BorderLayout())
    private val mapInfoContainer = Container(BoxLayout(Axis.Y, FillMode.Even))
    //navigation panel
    private val navContainer = Container(BorderLayout())
    private val orbitAction = object: com.simsilica.lemur.Action("Orbit") {
        override fun execute(source: Button?) {orbitSelection()}
    }
    private val approachAction = object: com.simsilica.lemur.Action("Approach"){
        override fun execute(source: Button?) {approachSelection()}
    }
    init{
        dashboard.addChild(equipmentPanel, BorderLayout.Position.Center)
        dashboard.addChild(mapContainer, BorderLayout.Position.East)
        dashboard.addChild(navContainer, BorderLayout.Position.West)
    }
    private lateinit var throttle : VersionedReference<Double>
    //
    private lateinit var mapper: InputMapper
    private lateinit var data: EntityData
    //TODO:Remove direct references to action and sensor systems, these should be server-side only
    private lateinit var sensorSys: SensorSystem
    //
    var playerId : EntityId? = null
    private var playerShip: WatchedEntity? = null
    private var target: WatchedEntity? = null
    private var focusedEntity: EntityId? = null

    override fun initialize(_app: Application) {
        val app = _app as SpaceTraderApp
        data = getState(ClientDataState::class.java).entityData
        sensorSys = app.serverManager.get(SensorSystem::class.java)
        shipEquipment = EquipmentContainer(data)
        shipEquipment.start()
        //focus
        EventBus.addListener(this, EntityFocusEvent.entityFocusLost, EntityFocusEvent.entityFocusGained)
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
        //Equipment Panel

        //minimap info
        val mapInfoName = Label("")
        mapInfoName.name = "NAME"
        mapInfoContainer.addChild(mapInfoName)
        mapContainer.addChild(mapInfoContainer, BorderLayout.Position.North)
        //minimap
        val mapState = getState(LocalMapState::class.java)
        val mapPanel = mapState.getMap()
        val mapSize = Vector3f(220f, 220f, 1f)
        mapContainer.addChild(mapPanel, BorderLayout.Position.Center)
        mapContainer.preferredSize = mapSize
        mapContainer.localTranslation = Vector3f(screenWidth-mapSize.x, mapSize.y, 0f)
        //nav panel
        val throttleModel = DefaultRangedValueModel(0.0, 1.0, 1.0)
        throttle = throttleModel.createReference()
        val throttleSlider = Slider(throttleModel, Axis.Y)
        throttleSlider.incrementButton.removeFromParent()
        throttleSlider.decrementButton.removeFromParent()
        navContainer.addChild(throttleSlider, BorderLayout.Position.East)
        //nav options
        val navOptions = Container(BoxLayout(Axis.Y, FillMode.Even))
        val orbitButton = ActionButton(orbitAction)
        val approachButton = ActionButton(approachAction)

        navOptions.addChild(orbitButton)
        navOptions.addChild(approachButton)
        navContainer.addChild(navOptions, BorderLayout.Position.Center)
        //add all our panels to the single dashboard
        //let's go for screen width and .25 screen height
        val dashY = screenHeight*0.25f
        dashboard.preferredSize = Vector3f(screenWidth.toFloat(), dashY, 1f)
        dashboard.localTranslation = Vector3f(0f, dashY, 0f)
        hudNode.attachChild(dashboard)
        //keyboard shortcuts
        mapper = GuiGlobals.getInstance().inputMapper
        mapper.addStateListener(this, SHIP_NEXT_TARGET)
        println("Ship Hud Enabled")
        targetMap = TargetContainer(data, application.camera)
    }

    override fun cleanup(app: Application?) {
        //keyboard shortcuts
        mapper.removeStateListener(this, SHIP_NEXT_TARGET)
    }

    override fun onEnable() {
        val app = application as SpaceTraderApp
        app.guiNode.attachChild(hudNode)
        mapper.activateGroup(SHIP_INPUT_GROUP)
        targetMap.start()
    }

    override fun onDisable() {
        hudNode.removeFromParent()
        mapper.deactivateGroup(SHIP_INPUT_GROUP)
        shipEquipment.stop()
    }

    override fun update(tpf: Float) {
        playerId?.let { watchPlayer(it) }
        if(playerShip != null){
            //do things with gui input
            if(throttle.update()) {
                EventBus.publish(ThrottleOrderEvent.setThrottle, ThrottleOrderEvent(playerShip!!.id, throttle.get()))
            }
        }
        //do ship equipment processing
        val simTime = getState(ClientState::class.java).approxSimTime
        shipEquipment.update(simTime)
        if(playerShip?.applyChanges() == true){
            //println("${shipEquipment.size}")
            //does player have a target
            val tgtId = playerShip?.get(TargetLock::class.java)?.targetId
            if(tgtId != target?.id){
                //our target has changed or been lost
                target?.release()
                target = if(tgtId == null) null else data.watchEntity(tgtId, Position::class.java, Name::class.java)
            }
            updatePlayerGui(playerShip!!)
        }
        target?.applyChanges()
        targetMap.update()
    }

    //TODO: All event publishing is currently avoiding a client/server communication problem that needs fixed asap
    fun orbitSelection(){
        val popState = getState(PopupState::class.java)
        val popup = object: RangePopup(0.0,100.0,25.0){
            override fun accept(value: Double) {
                focusedEntity?.let{
                    EventBus.publish(OrbitOrderEvent.orbitTarget, OrbitOrderEvent(playerShip!!.id, focusedEntity!!, value))
                }
            }
        }
        popState.centerInGui(popup)
        popState.showModalPopup(popup)
    }

    fun approachSelection(){
        focusedEntity?.let {
            EventBus.publish(ApproachOrderEvent.approachTarget, ApproachOrderEvent(playerShip!!.id, focusedEntity!!, 0.0))
        }
    }

    private fun updatePlayerGui(playerShip: WatchedEntity){
        val velocity = playerShip.get(Velocity::class.java)?.velocity ?: Vec3d(0.0,0.0,0.0)
        val floatFormat = "%.1f"
        velocityIndicator.text = "${floatFormat.format(velocity.length())} M/S"
        val energy = playerShip.get(Energy::class.java)?.curEnergy ?: 0.0
        energyGauge.text = "$energy% Energy"
        //val targetName = target?.get(Name::class.java)?.name ?: ""
        //(equipmentPanel.getChild(HUD_SELECTION_NAME) as Label).text = targetName
    }

    private fun watchPlayer(id: EntityId){
        playerShip?.release()
        playerShip = data.watchEntity(id, Position::class.java, Energy::class.java, Velocity::class.java,
            TargetLock::class.java)
        shipEquipment.resetFilter(ParentFilter(id))
        println("Watching player $id")
        playerId = null
    }

    /**
     * Target the next-furthest target until the furthest target has been found, then target the nearest
     */
    private fun nextTarget(){
        println("Searching for next furthest target")
        val pos = playerShip?.get(Position::class.java)?.position ?: return
        val farTgtId = targetMap.getNext(pos)
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
        targetMap.getNearest(pos)?.let { selectTarget(it) }
    }

    /**
     * Set the active target to the given id
     */
    private fun selectTarget(targetId: EntityId?){
        if(!sensorSys.acquireLock(playerShip?.id!!, targetId!!)) return
    }

    fun entityFocusLost(evt:EntityFocusEvent){
        selectId(null)
    }

    fun entityFocusGained(evt:EntityFocusEvent){
        selectId(evt.id)
    }

    private fun selectId(id: EntityId?){
        application.enqueue {
            focusedEntity = id
            //we should wait for update to do this :/
            val name: String = if (id == null) "" else data.getComponent(id, Name::class.java).name
            (mapInfoContainer.getChild("NAME") as Label).text = name
            //set actions that require a selection to enabled/disabled
            val enabled = (id != null)
            orbitAction.isEnabled = enabled
            approachAction.isEnabled = enabled
            println("Map item $id selected, buttons enabled:$enabled")
        }
    }

    /**
     * Keyboard Input
     */
    override fun valueChanged(func: FunctionId?, value: InputState?, tpf: Double) {
        when(func){
            SHIP_NEXT_TARGET -> {
                if(InputState.Positive != value) return
                if(focusedEntity == null){
                    nextTarget()
                } else{
                    selectTarget(focusedEntity)
                }
            }
        }
    }

    private inner class EquipmentContainer(data:EntityData):EntityContainer<EquipmentUIElement>(data,
        ParentFilter(null), Parent::class.java, Activate::class.java, Name::class.java, CycleTimer::class.java){
        override fun addObject(e: Entity): EquipmentUIElement {
            println("Adding $e")
            //make a mini container to hold the equipment and all it's data
            val eqpCont = Container(BorderLayout())
            eqpCont.addChild(Label(e.get(Name::class.java)!!.name), BorderLayout.Position.North)
            val eqpButton = Checkbox("")
            eqpButton.isChecked = e.get(Activate::class.java)!!.active
            //TODO:A form of client requests to the server to fix this
            //eqpButton.addClickCommands { e.set(Activate(eqpButton.isChecked)) }
            eqpCont.addChild(eqpButton, BorderLayout.Position.Center)
            equipmentPanel.addChild(eqpCont)
            val cycleTimer = e.get(CycleTimer::class.java)!!
            val cycleBar = ProgressBar(DefaultRangedValueModel(0.0,cycleTimer.duration, 0.0))
            eqpCont.addChild(cycleBar, BorderLayout.Position.South)
            return EquipmentUIElement(e.id, eqpCont, eqpButton, cycleBar, cycleTimer.nextCycle)
        }

        override fun updateObject(eqp: EquipmentUIElement, e: Entity) {
            //update checkbox and progress slider
            eqp.activeButton.isChecked=e.get(Activate::class.java).active
            eqp.cycleEnd=e.get(CycleTimer::class.java).nextCycle
            //eqp.cycleProg.setUserData("CycleEnd", e.get(CycleTimer::class.java).nextCycle)
        }

        override fun removeObject(eqp: EquipmentUIElement, e: Entity?) {
            equipmentPanel.removeChild(eqp.equipPane)
        }

        fun update(time:Long): Boolean {
            val changes = super.update()
            array.forEach {
                val deltaSeconds = max(0.0,(it.cycleEnd-time)*NANOS_TO_SECONDS)
                it.cycleProg.progressValue=it.cycleProg.model.maximum-deltaSeconds
                it.cycleProg.message=String.format("%.1f", deltaSeconds)
            }
            return changes
        }

        fun resetFilter(f:ComponentFilter<out EntityComponent>){
            this.setFilter(f)
        }
    }

    private inner class TargetContainer(data:EntityData, val cam:Camera):EntityContainer<TargetUIElement>(data, Position::class.java){
        override fun addObject(e: Entity): TargetUIElement {
            val pane = HudBracket()
            val pos = e.get(Position::class.java)
            val tgt = TargetUIElement(e.id,pane,pos)
            pane.addControl(WorldToCamControl(tgt, cam))
            hudNode.attachChild(pane)
            return tgt
        }

        override fun updateObject(tgt: TargetUIElement, e: Entity) {
            tgt.pos=e.get(Position::class.java)
        }

        override fun removeObject(tgt: TargetUIElement, e: Entity) {
            tgt.uiPane.removeFromParent()
        }

        fun getNearest(refPos:Vec3d): EntityId?{
            var nearestDist = Double.MAX_VALUE
            var nearTgtId: EntityId? = target?.id
            array.forEach {
                if(it.id == playerShip?.id) return@forEach
                if(it.id == nearTgtId) return@forEach
                val itDist = it.pos.position.distanceSq(refPos)
                if(itDist >= nearestDist) return@forEach
                nearestDist = itDist
                nearTgtId = it.id
            }
            return nearTgtId
        }

        fun getNext(refPos:Vec3d): EntityId?{
            println("Searching for next furthest target")
            val farTgtDist = target?.get(Position::class.java)?.position?.distanceSq(refPos) ?: 0.0
            var nearestDist = Double.MAX_VALUE
            var farTgtId: EntityId? = target?.id
            //find the next-furthest target from us currently
            array.forEach {
                if(it.id == playerShip?.id) return@forEach
                if(it.id == farTgtId) return@forEach
                val itDist = it.pos.position.distanceSq(refPos)
                if(itDist > farTgtDist && itDist < nearestDist){
                    nearestDist = itDist
                    farTgtId = it.id
                }
            }
            return farTgtId
        }
    }

    private class WorldToCamControl(val tgt:TargetUIElement, val cam:Camera): AbstractControl(){
        private val offset:Vector3f = tgt.uiPane.preferredSize.mult(Vector3f(-0.5f,0.5f,1f))
        override fun controlUpdate(tpf: Float) {
            spatial.localTranslation = cam.getScreenCoordinates(tgt.pos.position.toVector3f()).add(offset)
        }

        override fun controlRender(rm: RenderManager?, vp: ViewPort) {}
    }
}

class HudBracket:Panel(32f,32f, ElementId(ELEMENT_ID), null){
    companion object{
        const val ELEMENT_ID = "bracket"
    }
}