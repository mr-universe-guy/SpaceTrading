package `fun`.familyfunforce.cosmos.ui

import `fun`.familyfunforce.cosmos.SpaceTraderApp
import `fun`.familyfunforce.cosmos.event.InspectEvent
import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.scene.Node
import com.simsilica.event.EventBus
import com.simsilica.event.EventListener
import com.simsilica.event.EventType
import com.simsilica.lemur.*
import com.simsilica.lemur.component.BorderLayout
import com.simsilica.lemur.component.BoxLayout

class InspectionState:BaseAppState(), EventListener<InspectEvent> {
    private lateinit var guiNode: Node
    private val inspectorPane = Container(BorderLayout())
    private val inspectorTitleBar = Container(BoxLayout(Axis.X, FillMode.First))
    private val inspectorTitle = Label("Inspector")
    private val inspectorCloseBtn = Button("X")
    init{
        inspectorTitleBar.addChild(inspectorTitle)
        inspectorTitleBar.addChild(inspectorCloseBtn)
        inspectorCloseBtn.addClickCommands { inspectorPane.removeFromParent() }
        inspectorPane.addChild(inspectorTitleBar, BorderLayout.Position.North)
    }
    private val statsContainer = Container(BoxLayout(Axis.Y, FillMode.Even))
    init{inspectorPane.addChild(statsContainer, BorderLayout.Position.Center)}
    private var curInspectable: Inspectable? = null
        set(v) {
            field=v
            application.enqueue { populateInspector() }
            println(v?.getInfo())
        }

    private fun populateInspector() {
        statsContainer.clearChildren()
        curInspectable ?: run{
            inspectorPane.removeFromParent()
            return
        }
        //populate inspector pane
        curInspectable!!.getInfo().entries.forEach {
            statsContainer.addChild(Label("${it.key}:${it.value}"))
        }
        GuiGlobals.getInstance().popupState.centerInGui(inspectorPane)
        guiNode.attachChild(inspectorPane)
    }

    override fun initialize(_app: Application) {
        val app = _app as SpaceTraderApp
        guiNode = app.guiNode
    }

    override fun cleanup(app: Application?) {
        inspectorPane.removeFromParent()
    }

    override fun onEnable() {
        EventBus.addListener(InspectEvent.InspectionRequest, this)
    }

    override fun onDisable() {
        EventBus.removeListener(InspectEvent.InspectionRequest, this)
    }

    override fun newEvent(type: EventType<InspectEvent>, event: InspectEvent) {
        application.enqueue { curInspectable=(event.inspectable) }
    }
}

interface Inspectable{
    /**
     * Get the inspectable info in a string/value pair. The inspector will evaluate the objects and use the correct
     * renderer per the InspectionUIState
     */
    fun getInfo():Map<String,Any>
}