package `fun`.familyfunforce.cosmos.ui

import `fun`.familyfunforce.cosmos.loadout.*
import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.app.state.BaseAppState
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.simsilica.lemur.*
import com.simsilica.lemur.Action
import com.simsilica.lemur.component.BorderLayout
import com.simsilica.lemur.component.BoxLayout
import com.simsilica.lemur.core.GuiControl
import com.simsilica.lemur.core.VersionedList
import com.simsilica.lemur.event.CursorButtonEvent
import com.simsilica.lemur.event.CursorEventControl
import com.simsilica.lemur.event.DragHandler
import javax.swing.JFileChooser

class LoadoutEditorState: BaseAppState() {
    private val editorNode = Node("Editor")
    private val vehicleNode = Node("Vehicle")
    private val equipmentNode = Node("Equipment")
    private val topMenu = Container(BoxLayout(Axis.X, FillMode.None))
    //create loadout
    private val createLoadoutPopup = VehicleSelectorPopup()
    private val createLoadoutAction = object: Action("New") {override fun execute(source: Button?) {
            val pop = GuiGlobals.getInstance().popupState
            pop.centerInGui(createLoadoutPopup)
            pop.showModalPopup(createLoadoutPopup)
        }}
    private val saveAction = object: Action("Save"){override fun execute(source: Button?) {
            val fileFinder = JFileChooser()
            if(fileFinder.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return
            exportLoadout(activeLoadout!!, fileFinder.selectedFile)
        }}
    private var activeVehicle: Vehicle? = null
    private var activeLoadout: Loadout? = null
    init{
        editorNode.attachChild(vehicleNode)
        editorNode.attachChild(equipmentNode)
    }

    override fun initialize(app: Application) {
        val screenWidth = app.camera.width
        val screenHeight = app.camera.height
        //menu bar
        val menuHeight = 20f
        topMenu.preferredSize = Vector3f(screenWidth.toFloat(), menuHeight, 1f)
        topMenu.setLocalTranslation(0f,screenHeight.toFloat(),0f)
        topMenu.addChild(ActionButton(createLoadoutAction))
        topMenu.addChild(ActionButton(saveAction))
        editorNode.attachChild(topMenu)
        //loadout popup
        //get all vehicles currently in the cache and make a list box
        val vehicleList = ListBox(VersionedList(getVehicleCache().keys.toList()))
        vehicleList.name = "VicList"
        GuiGlobals.getInstance().popupState.centerInGui(createLoadoutPopup)
        //equipment selection
        //val equipmentList = ListBox(VersionedList(getEquipmentCache().values.toMutableList()), EquipmentRenderer(), "")
        val equipmentList = Container(BoxLayout(Axis.Y, FillMode.None))
        getEquipmentCache().values.forEach { equipmentList.addChild(EquipmentPanel(it, null)) }
        equipmentList.preferredSize = Vector3f(200f, screenHeight.toFloat()-menuHeight, 1f)
        equipmentList.setLocalTranslation(0f, screenHeight.toFloat()-menuHeight, 0f)
        equipmentNode.attachChild(equipmentList)
    }

    private fun createNewLoadout(vehicleId : String){
        val vehicle = getVehicleCache()[vehicleId]!!
        activeVehicle = vehicle
        val loadout = Loadout(vehicle.name, vehicle.vehicleId)
        activeLoadout = loadout
        println("Active loadout set to $loadout")
        //fill out gui elements
        vehicleNode.detachAllChildren()
        //for now, we're going to have one box hold all the sections but in the future sections should be offset to the artwork
        val sectionsBox = Container(BoxLayout(Axis.Y, FillMode.None))
        vehicle.sections.forEach {
            sectionsBox.addChild(SectionContainer(it.value))
        }
        GuiGlobals.getInstance().popupState.centerInGui(sectionsBox)
        vehicleNode.attachChild(sectionsBox)
    }

    override fun cleanup(app: Application?) {
    }

    override fun onEnable() {
        val app = application as SimpleApplication
        app.guiNode.attachChild(editorNode)
    }

    override fun onDisable() {
        editorNode.removeFromParent()
    }

    private inner class VehicleSelectorPopup: OptionPanel("Select Vehicle"){
        val vehicleList = ListBox(VersionedList(getVehicleCache().keys.toList()))
        val acceptAction = object: Action("Accept") {override fun execute(source: Button?) {
                createNewLoadout(vehicleList.selectedItem)}}
        init{
            container.addChild(vehicleList)
            setOptions(EmptyAction("Cancel"), acceptAction)
        }
    }

    private inner class SectionContainer(val section: Section) : Panel(){
        val infoLabel = Label("")
        val equipment = Container(BoxLayout(Axis.Y, FillMode.None))
        init{
            val uic = getControl(GuiControl::class.java)
            val layout = BorderLayout()
            uic.setLayout(layout)
            layout.addChild(infoLabel, BorderLayout.Position.North)
            layout.addChild(equipment, BorderLayout.Position.Center)
            updateSectionText()
            //listeners
            val cec = CursorEventControl()
            addControl(cec)
        }

        fun updateSectionText(){
            val name = section.name
            val freeSlots = activeLoadout?.getFreeSlots(section)
            val totalSlots = section.slots
            infoLabel.text = "$name $freeSlots:$totalSlots"
        }

        fun updateSectionEquipment(){
            equipment.clearChildren()
            val equipKeys = activeLoadout!!.getEquipmentInSection(section.name)
            val cache = getEquipmentCache()
            equipKeys.forEach {
                val equip = cache[it]!!
                val panel = EquipmentPanel(equip, section)
                equipment.addChild(panel)
            }
            updateSectionText()
        }
    }

    private inner class EquipmentDragHandler(val equipment: Equipment, val section: Section?): DragHandler() {
        override fun cursorButtonEvent(event: CursorButtonEvent, target: Spatial?, capture: Spatial?) {
            super.cursorButtonEvent(event, target, capture)
            if(!event.isPressed) onRelease(target, capture)
        }
        private fun onRelease(target: Spatial?, capture: Spatial?){
            if(capture !is EquipmentPanel) return
            //auto-return dragged component to it's parent
            capture.getControl(GuiControl::class.java).invalidate()
            if(section != null){
                //we are moving this from one section to another, delete this from it's original parent
                activeLoadout!!.removeEquipment(section.name, equipment.equipmentId)
                (capture.parent.parent as SectionContainer).updateSectionEquipment()
            }
            if(target !is SectionContainer) return
            println("Equip $equipment to section in ${target.section} on loadout $activeLoadout")
            if(activeLoadout!!.attachEquipment(target.section.name, equipment)){
                println("$equipment equipped to ${target.section}")
            } else{
                println("Cannot put $equipment in ${target.section}")
            }
            target.updateSectionEquipment()
        }
    }

    private inner class EquipmentPanel(equipment: Equipment, section: Section?): Panel(){
        val nameLabel = Label(equipment.name)
        init{
            val layout = BorderLayout()
            val uic = getControl(GuiControl::class.java)
            uic.setLayout(layout)
            layout.addChild(nameLabel, BorderLayout.Position.Center)
            val cc = CursorEventControl()
            addControl(cc)
            cc.addMouseListener(EquipmentDragHandler(equipment, section))
        }
    }
}