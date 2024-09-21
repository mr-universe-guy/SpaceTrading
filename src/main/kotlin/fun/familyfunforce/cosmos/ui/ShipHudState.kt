package `fun`.familyfunforce.cosmos.ui

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.input.MouseInput
import com.jme3.input.event.MouseButtonEvent
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import com.simsilica.es.*
import com.simsilica.event.EventBus
import com.simsilica.lemur.*
import com.simsilica.lemur.component.BorderLayout
import com.simsilica.lemur.component.BoxLayout
import com.simsilica.lemur.component.ColoredComponent
import com.simsilica.lemur.core.GuiControl
import com.simsilica.lemur.core.VersionedReference
import com.simsilica.lemur.event.DefaultMouseListener
import com.simsilica.lemur.event.MouseEventControl
import com.simsilica.lemur.event.PopupState
import com.simsilica.lemur.input.FunctionId
import com.simsilica.lemur.input.InputMapper
import com.simsilica.lemur.input.InputState
import com.simsilica.lemur.input.StateFunctionListener
import com.simsilica.lemur.style.ElementId
import com.simsilica.mathd.Vec3d
import `fun`.familyfunforce.cosmos.*
import `fun`.familyfunforce.cosmos.Name
import `fun`.familyfunforce.cosmos.event.ApproachOrderEvent
import `fun`.familyfunforce.cosmos.event.EquipmentToggleEvent
import `fun`.familyfunforce.cosmos.event.OrbitOrderEvent
import `fun`.familyfunforce.cosmos.event.ThrottleOrderEvent
import `fun`.familyfunforce.cosmos.systems.ClientDataState
import `fun`.familyfunforce.cosmos.systems.SensorSystem
import kotlin.math.max

/**
 * A state to manage the interactive ship Hud and UI.
 * The ship hud consists of both lemur and jfx elements
 */
class ShipHudState: BaseAppState(), StateFunctionListener{
    private lateinit var targetMap: TargetContainer
    private lateinit var shipEquipment: EquipmentContainer
    private lateinit var localObjectEntityContainer: LocalObjectContainer
    private data class TargetUIElement(val id:EntityId, val uiPane:HudBracket, val tgtSpatial:Spatial, var pos:Position)
    private data class EquipmentUIElement(val id:EntityId, val equipPane:Container, val activeButton:Checkbox,
                                          val cycleProg:ProgressBar, var cycleEnd:Long)
    private data class LocalObjectInfo(val id:EntityId, val uiRow:HudRow, var name:String, var pos: Vec3d)

    //lemur hud elements
    private val hudNode = Node("Hud_Gui")
    private val energyGauge= Label("XXX% Energy")
    private val velocityIndicator= Label("XXXX m/s")

    //all panels go on the dash
    private val dashboard = Container(BorderLayout())
    //interaction panel
    private val equipmentPanel = Container(BoxLayout(Axis.X, FillMode.None))
    //status panel
    private val statusPanel = Container(BoxLayout(Axis.X, FillMode.None))
    private val healthBar = ProgressBar()
    private val heatBar = ProgressBar()
    //mini map container
    private val mapContainer = Container(BorderLayout())
    private val mapInfoContainer = Container(BoxLayout(Axis.Y, FillMode.Even))
    //navigation panel
    private val navContainer = Container(BorderLayout())
    private val orbitAction = object: com.simsilica.lemur.Action("Orbit") {
        override fun execute(source: Button?) {focusedEntity?.let { orbitId(it) }}
    }
    private val approachAction = object: com.simsilica.lemur.Action("Approach"){
        override fun execute(source: Button?) {focusedEntity?.let { approachId(it) }}
    }
    //Window Panes
    private lateinit var localObjectsListContainer: Container

    init{
        dashboard.addChild(equipmentPanel, BorderLayout.Position.Center)
        dashboard.addChild(mapContainer, BorderLayout.Position.East)
        dashboard.addChild(navContainer, BorderLayout.Position.West)
        dashboard.addChild(statusPanel, BorderLayout.Position.North)
    }
    private lateinit var throttle : VersionedReference<Double>
    //
    private lateinit var mapper: InputMapper
    private lateinit var data: EntityData
    private lateinit var visuals: VisualState
    //TODO:Remove direct references to action and sensor systems, these should be server-side only
    private lateinit var sensorSys: SensorSystem
    //
    private lateinit var playerId : VersionedReference<EntityId?>
    private var playerShip: WatchedEntity? = null
    private var target: WatchedEntity? = null
    private var focusedEntity: EntityId? = null

    companion object{
        private const val HUD_COLORS_ID = "space.hudcolors"
        lateinit var defaultColor:ColorRGBA
        lateinit var focusColor:ColorRGBA
        lateinit var targetColor:ColorRGBA

        const val FOCUSED = 1
        const val TARGETED = 2

        fun getObjectTint(flags:Int): ColorRGBA{
            val col = when(flags){
                FOCUSED -> focusColor
                TARGETED -> targetColor
                TARGETED or FOCUSED -> targetColor
                else -> defaultColor
            }
            return col
        }
    }

    override fun initialize(_app: Application) {
        playerId = getState(PlayerIdState::class.java).watchPlayerId()
        val app = _app as SpaceTraderApp
        data = getState(ClientDataState::class.java).entityData
        sensorSys = app.serverManager.get(SensorSystem::class.java)
        visuals = getState(VisualState::class.java)
        shipEquipment = EquipmentContainer(data)
        shipEquipment.start()

        //events
        EventBus.addListener(this, EntityFocusEvent.entityFocusChanged, TargetingEvent.targetChanged)
        //prep lemur
        val hudColors = GuiGlobals.getInstance().styles.getAttributes(ElementId(HUD_COLORS_ID))
        defaultColor = hudColors.get("defaultColor")
        focusColor = hudColors.get("focusColor")
        targetColor = hudColors.get("targetColor")
        println("SHIP HUD STATE INIT HUD COLORS:$hudColors, WITH VALUE:$focusColor")
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
        //top toolbar
        val toolbarContainer = Container(BoxLayout(Axis.X, FillMode.Even))
        toolbarContainer.preferredSize = Vector3f(600f, 30f, 0f)
        toolbarContainer.localTranslation = Vector3f(0f,screenHeight.toFloat(),0f)
        //local objects
        val localObjectsToggle = Button("Local Objects")
        localObjectsListContainer = Container(BoxLayout(Axis.Y, FillMode.None))
        val localObjectsWindow = WindowPane("Local Objects", localObjectsListContainer)
        localObjectsWindow.localTranslation = Vector3f(screenWidth/2f, screenHeight/2f, 0f)
        localObjectsToggle.addClickCommands {
            hudNode.attachChild(localObjectsWindow)
        }
        localObjectEntityContainer = LocalObjectContainer(data, localObjectsListContainer)
        localObjectEntityContainer.start()

        toolbarContainer.addChild(localObjectsToggle)
        //system objects
        //end toolbar
        hudNode.attachChild(toolbarContainer)
        //Status Panel
        statusPanel.addChild(heatBar)
        statusPanel.addChild(healthBar)
        //end status
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
        if(playerId.update()){
            watchPlayer(playerId.get()!!)
        }
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
            val tgtId = playerShip?.get(TargetId::class.java)?.targetId
            if(tgtId != target?.id){
                //our target has changed or been lost
                target?.release()
                target = if(tgtId == null) null else data.watchEntity(tgtId, Position::class.java, Name::class.java)
            }
            target?.applyChanges()
            updatePlayerGui(playerShip!!)
        }
        targetMap.update()
        localObjectEntityContainer.update()
    }

    //TODO: All event publishing is currently avoiding a client/server communication problem that needs fixed asap
    fun orbitId(targetId: EntityId){
        val popState = getState(PopupState::class.java)
        val popup = object: RangePopup(0.0,100.0,25.0){
            override fun accept(value: Double) {
                playerShip?.let {EventBus.publish(OrbitOrderEvent.orbitTarget, OrbitOrderEvent(it.id, targetId, value))}
            }
        }
        popState.centerInGui(popup)
        popState.showModalPopup(popup)
    }

    fun approachId(targetId: EntityId){
        playerShip?.let {EventBus.publish(ApproachOrderEvent.approachTarget, ApproachOrderEvent(it.id, targetId, 0.0))}
    }

    private fun updatePlayerGui(playerShip: WatchedEntity){
        val velocity = playerShip.get(Velocity::class.java)?.velocity ?: Vec3d(0.0,0.0,0.0)
        val floatFormat = "%.1f"
        velocityIndicator.text = "${floatFormat.format(velocity.length())} M/S"
        val energy = playerShip.get(Energy::class.java)?.curEnergy ?: 0.0
        energyGauge.text = "$energy% Energy"
        //val targetName = target?.get(Name::class.java)?.name ?: ""
        //(equipmentPanel.getChild(HUD_SELECTION_NAME) as Label).text = targetName
        val hp = playerShip.get(HealthPoints::class.java)
        val dmg = playerShip.get(Damage::class.java)
        healthBar.message = "${hp.armor-dmg.armorDamage}/${hp.armor}"
        healthBar.progressPercent = (hp.armor-dmg.armorDamage).toDouble()/hp.armor.toDouble()

        val heat = playerShip.get(Heat::class.java).heat
        val heatLimit = playerShip.get(HeatLimit::class.java).limit
        heatBar.message = "$heat/$heatLimit"
        heatBar.progressPercent = heat.toDouble()/heatLimit.toDouble()
    }

    private fun watchPlayer(id: EntityId){
        playerShip?.release()
        //todo: clear hud related things
        playerShip = data.watchEntity(
            id,
            Position::class.java,
            Energy::class.java,
            Velocity::class.java,
            TargetId::class.java,
            TargetTrack::class.java,
            HealthPoints::class.java,
            Damage::class.java,
            Heat::class.java,
            HeatLimit::class.java
        )
        shipEquipment.resetFilter(ParentFilter(id))
        println("Watching player $id")
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

    fun targetChanged(evt: TargetingEvent){
        if(evt.acquired){
            targetMap.targetAcquired(evt, true)
            localObjectEntityContainer.entityTargetAcquired(evt)
        } else{
            targetMap.targetAcquired(evt, false)
            localObjectEntityContainer.entityTargetLost(evt)
        }
    }

    fun entityFocusChanged(evt:EntityFocusEvent){
        focusId(evt.id)
        targetMap.entityFocusChanged(evt)
        localObjectEntityContainer.entityFocusChanged(evt)
    }

    private fun focusId(id: EntityId?){
        application.enqueue {
            focusedEntity = id
            //we should wait for update to do this :/
            val name: String = if (id == null) "" else data.getComponent(id, Name::class.java)?.name ?: ""
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
        ParentFilter(null), Parent::class.java, EquipmentPower::class.java, Name::class.java, CycleTimer::class.java){
        override fun addObject(e: Entity): EquipmentUIElement {
            println("Adding $e")
            //make a mini container to hold the equipment and all it's data
            val eqpCont = Container(BorderLayout())
            eqpCont.addChild(Label(e.get(Name::class.java)!!.name), BorderLayout.Position.North)
            val eqpButton = Checkbox("")
            eqpButton.isChecked = e.get(EquipmentPower::class.java)?.powered ?: false
            eqpButton.addClickCommands {EventBus.publish(EquipmentToggleEvent.setEquipmentPower, EquipmentToggleEvent(e.id, eqpButton.isChecked))}
            eqpCont.addChild(eqpButton, BorderLayout.Position.Center)
            equipmentPanel.addChild(eqpCont)
            val cycleTimer = e.get(CycleTimer::class.java)!!
            val cycleBar = ProgressBar(DefaultRangedValueModel(0.0,cycleTimer.duration, 0.0))
            eqpCont.addChild(cycleBar, BorderLayout.Position.South)
            return EquipmentUIElement(e.id, eqpCont, eqpButton, cycleBar, cycleTimer.nextCycle)
        }

        override fun updateObject(eqp: EquipmentUIElement, e: Entity) {
            //update checkbox and progress slider
            eqp.activeButton.isChecked=e.get(EquipmentPower::class.java).powered
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

    private inner class LocalObjectContainer(data:EntityData, val list: Container): EntityContainer<LocalObjectInfo>(data, Position::class.java, Name::class.java){
        fun entityFocusChanged(evt: EntityFocusEvent){
            val row = getObject(evt.id)
            if(evt.focused){
                row.uiRow.setFlags(FOCUSED)
            } else{
                row.uiRow.unsetFlags(FOCUSED)
            }
        }

        fun entityTargetAcquired(evt: TargetingEvent){
            getObject(evt.id).uiRow.setFlags(TARGETED)
        }

        fun entityTargetLost(evt: TargetingEvent){
            getObject(evt.id).uiRow.unsetFlags(TARGETED)
        }

        override fun addObject(e: Entity): LocalObjectInfo {
            val pos = e.get(Position::class.java).position
            val name = e.get(Name::class.java).name
            val dist = if(playerShip == null){0.0} else{
                e.get(Position::class.java).position.distance(playerShip!!.get(Position::class.java).position)
            }
            //TODO: names don't need to be stores per column, the name will be defined by the table
            val columns = arrayOf<HudColumn<Any>>(HudColumn("Name", name), HudColumn("Distance", "$dist"))
            val row = HudRow(e.id, columns)
            //interaction buttons

            list.addChild(row)
            //TODO: add versioned object watcher to row as a control to update info as it changes
            return LocalObjectInfo(e.id, row, name, pos)
        }

        override fun removeObject(info: LocalObjectInfo, e: Entity) {
            info.uiRow.removeFromParent()
        }

        override fun updateObject(info: LocalObjectInfo, e: Entity) {
            info.pos = e.get(Position::class.java).position
        }

        override fun update(): Boolean {
            val updatesAvailable = super.update()
            if(playerShip != null){
                val playerPos = playerShip?.get(Position::class.java)?.position ?: Vec3d(0.0,0.0,0.0)
                for(obj in array){
                    val dist = "%.0f".format(obj.pos.distance(playerPos))
                    obj.uiRow.setColumn(1, dist)
                }
            }
            return updatesAvailable
        }
    }

    private inner class TargetContainer(data:EntityData, val cam:Camera):EntityContainer<TargetUIElement>(data, Position::class.java){
        fun targetAcquired(evt:TargetingEvent, acquired:Boolean){
            if(acquired) {
                getObject(evt.id)!!.uiPane.setFlags(TARGETED)
            } else{
                getObject(evt.id)?.uiPane?.unsetFlags(TARGETED)
            }
        }

        fun entityFocusChanged(evt:EntityFocusEvent){
            if(evt.focused){
                getObject(evt.id)!!.uiPane.setFlags(FOCUSED)
            } else{
                getObject(evt.id)?.uiPane?.unsetFlags(FOCUSED)
            }

        }

        override fun addObject(e: Entity): TargetUIElement {
            val eid = e.id
            val pane = HudBracket(eid)
            val pos = e.get(Position::class.java)
            val spat = visuals.getSpatialFromId(eid)!!//hopefully we catch any failures here or I'ma be annoyed
            val tgt = TargetUIElement(e.id,pane,spat,pos)
            pane.addControl(WorldToCamControl(tgt, cam))
            hudNode.attachChild(pane)
            return tgt
        }

        override fun updateObject(tgt: TargetUIElement, e: Entity) {
            //TODO:Have the HUD bracket attach to the target spatial!
            //tgt.pos=e.get(Position::class.java)
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
            //spatial.localTranslation = cam.getScreenCoordinates(tgt.pos.position.toVector3f()).add(offset)
            spatial.localTranslation = cam.getScreenCoordinates(tgt.tgtSpatial.worldTranslation).add(offset)
        }

        override fun controlRender(rm: RenderManager?, vp: ViewPort) {}
    }
}

class HudRow(val id: EntityId, private val dataColumns:Array<HudColumn<Any>>): Panel(ELEMENT_ID,null), UIFlaggable{
    private val labels:Array<Label>
    private var flags = 0

    companion object{
        val ELEMENT_ID = ElementId("hudrow")
        val CELL_ID = ELEMENT_ID.child("cell")
    }
    init {
        val gui = getControl(GuiControl::class.java)
        val layout = BoxLayout(Axis.X, FillMode.None)
        gui.setLayout(layout)

        labels = Array(dataColumns.size) { i ->
            val col = dataColumns[i]
            val label = Label("${col.label}: ${col.data}", CELL_ID)
            layout.addChild(label)
            return@Array label
        }

        addMouseListener(object: DefaultMouseListener(){
            override fun mouseButtonEvent(event: MouseButtonEvent, target: Spatial?, capture: Spatial?) {
                if(event.buttonIndex == MouseInput.BUTTON_LEFT){
                    EventBus.publish(EntityFocusEvent.entityFocusRequest, EntityFocusEvent(id, true))
                } else if(event.buttonIndex == MouseInput.BUTTON_RIGHT){
                    EventBus.publish(InteractMenuEvent.requestInteractMenu, InteractMenuEvent(worldTranslation, id))
                }
            }
        })
    }

    override fun setFlags(vararg flags: Int) {
        this.flags = UIFlaggable.setFlagState(this.flags, *flags)
        (background as ColoredComponent).color = ShipHudState.getObjectTint(this.flags)
    }

    override fun unsetFlags(vararg flags: Int) {
        this.flags = UIFlaggable.unsetFlagState(this.flags, *flags)
        (background as ColoredComponent).color = ShipHudState.getObjectTint(this.flags)
    }

    fun setColumn(index:Int, data:Any){
        val col = dataColumns[index]
        col.data = data
        labels[index].text = "${col.label}+:+${col.data}"
    }
}

data class HudColumn<dataType>(var label:String, var data:dataType)

class HudBracket(val id:EntityId):Panel(32f,32f, ElementId(ELEMENT_ID), null), UIFlaggable{
    private var bracketFlags = 0

    companion object{
        const val ELEMENT_ID = "bracket"
    }

    init {
        MouseEventControl.addListenersToSpatial(this, object: DefaultMouseListener(){
            override fun click(event: MouseButtonEvent, target: Spatial?, capture: Spatial?) {
                when(event.buttonIndex){
                    MouseInput.BUTTON_LEFT -> EventBus.publish(EntityFocusEvent.entityFocusRequest, EntityFocusEvent(id, true))
                    MouseInput.BUTTON_RIGHT -> EventBus.publish(InteractMenuEvent.requestInteractMenu, InteractMenuEvent(
                        Vector3f(event.x.toFloat(), event.y.toFloat(), 0f),id))
                }
            }
        })
    }

    override fun setFlags(vararg flags: Int) {
        bracketFlags = UIFlaggable.setFlagState(bracketFlags, *flags)
        (background as ColoredComponent).color = ShipHudState.getObjectTint(bracketFlags)
    }

    override fun unsetFlags(vararg flags: Int) {
        bracketFlags = UIFlaggable.unsetFlagState(bracketFlags, *flags)
        (background as ColoredComponent).color = ShipHudState.getObjectTint(bracketFlags)
    }
}

interface UIFlaggable{
    companion object{
        fun setFlagState(state:Int, vararg flags:Int): Int{
            if(flags.isEmpty()) return state
            return state or flags.fold(0) {total, f -> total or f}
        }

        fun unsetFlagState(state:Int, vararg flags:Int): Int{
            if(flags.isEmpty()) return state
            return state and flags.fold(0) {total, f -> total or f}.inv()
        }
    }

    fun setFlags(vararg flags:Int)
    fun unsetFlags(vararg flags:Int)
}

/**
 * A menu that updates in real time and displays all the ways in which you may interact with the given entity
 */
