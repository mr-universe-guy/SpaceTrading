package `fun`.familyfunforce.cosmos.ui

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.math.Vector3f
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.WatchedEntity
import com.simsilica.event.EventBus
import com.simsilica.event.EventType
import com.simsilica.lemur.*
import com.simsilica.lemur.Action
import com.simsilica.lemur.component.BorderLayout
import com.simsilica.lemur.component.BoxLayout
import com.simsilica.lemur.core.VersionedReference
import com.simsilica.lemur.event.PopupState
import `fun`.familyfunforce.cosmos.*
import `fun`.familyfunforce.cosmos.event.ApproachOrderEvent
import `fun`.familyfunforce.cosmos.event.OrbitOrderEvent
import java.util.function.Consumer

/**
 * Manages an interaction menu, it's hidden or visible status,
 * it's current focus and any active/deactive actions
 */
class InteractionMenuState: BaseAppState() {
    private lateinit var data:EntityData
    private lateinit var popupState:PopupState
    private lateinit var pid:VersionedReference<EntityId?>
    private var interactTarget:WatchedEntity? = null
    //nav
    private val navMenu = Container(BoxLayout(Axis.Y, FillMode.Even))
    private val navMenuAction:Action
    val approach = object:Action("Approach"){
        override fun execute(source: Button?) {
            if(pid.get() == null || interactTarget == null) return
            val consumer = Consumer<Double> { EventBus.publish(ApproachOrderEvent.approachTarget, ApproachOrderEvent(pid.get()!!, interactTarget!!.id, it)) }
            getRangeFromPopup(consumer)
        }
    }
    val orbit = object:Action("Orbit"){
        override fun execute(source: Button?) {
            if(pid.get() == null || interactTarget == null) return
            val consumer = Consumer<Double> { EventBus.publish(OrbitOrderEvent.orbitTarget, OrbitOrderEvent(pid.get()!!, interactTarget!!.id, it)) }
            getRangeFromPopup(consumer)
        }
    }
    init{
        navMenu.addChild(ActionButton(approach))
        navMenu.addChild(ActionButton(orbit))
        navMenuAction = object:DisplaySubMenuAction("Navigation", navMenu){
            override fun update() {
                val hasPosition = interactTarget?.get(Position::class.java)?.position != null
                pid.update()
                val pidExists = pid.get() != null
                approach.isEnabled = hasPosition && pidExists
                orbit.isEnabled = hasPosition && pidExists
            }
        }
    }

    //interact
    private val interactMenu = Container(BoxLayout(Axis.Y, FillMode.Even))
    private val interactMenuAction:Action
    init{
        val nameLabel = Label("Name:Unknown")
        interactMenu.addChild(nameLabel)
        interactMenuAction=object: DisplaySubMenuAction("Interaction", interactMenu) {
            override fun update() {
                nameLabel.text = interactTarget?.get(Name::class.java)?.name ?: "UNKNOWN"
            }
        }
    }

    //communications
    private val commMenu = Container(BoxLayout(Axis.Y, FillMode.Even))
    private val commMenuAction = object:DisplaySubMenuAction("Communication", commMenu){
        override fun update() {
            TODO("Not yet implemented")
        }
    }
    //private val interactPanel = OptionPanel("", navMenuAction, interactMenuAction, commMenuAction);
    private val interactPanel = InteractMenu("", navMenuAction, interactMenuAction, commMenuAction)

    override fun initialize(app: Application?) {
        data = (app as SpaceTraderApp).serverManager.get(DataSystem::class.java).entityData
        popupState = getState(PopupState::class.java)
        pid = getState(PlayerIdState::class.java).watchPlayerId()
    }

    override fun update(tpf: Float) {
        interactTarget?.applyChanges() //keep the target up to date
    }

    fun requestInteractMenu(evt:InteractMenuEvent){
        application.enqueue {
            interactTarget?.release()
            //set new interact target and watch it
            //also wait until watched entity is ready? dunno how to do that
            interactTarget = data.watchEntity(evt.id,Position::class.java, Name::class.java)
            //fill in menu options as they become available
            interactPanel.setLocalTranslation(evt.pos.x, evt.pos.y, 0f)
            //popupState.centerInGui(interactPanel)//TODO: have the popup originate from click location
            //populate interact panel
            interactPanel.setTitle(interactTarget?.get(Name::class.java)?.name ?: "Unknown")
            popupState.showPopup(interactPanel)
            //optionState.show(interactPanel)
        }
    }

    private fun getRangeFromPopup(consumer: Consumer<Double>){
        val rangePopup = object:RangePopup(0.0, 100.0, 50.0){
            override fun accept(value: Double) {
                consumer.accept(value)
            }
        }
        popupState.centerInGui(rangePopup)
        popupState.showModalPopup(rangePopup)
    }

    override fun cleanup(app: Application?) {
    }

    override fun onEnable() {
        EventBus.addListener(this, InteractMenuEvent.requestInteractMenu)
    }

    override fun onDisable() {
        EventBus.removeListener(this, InteractMenuEvent.requestInteractMenu)
        interactTarget?.release()
    }

    private class InteractMenu(_title:String, vararg _actions:Action):Container(BorderLayout()){
        val title = Label(_title)
        val actions = _actions
        val actionCont = Container(BoxLayout(Axis.Y, FillMode.Even))
        init{
            this.addChild(title, BorderLayout.Position.North)
            actions.forEach {
                actionCont.addChild(ActionButton(it))
            }
            addChild(actionCont, BorderLayout.Position.Center)
        }

        fun setTitle(text:String){
            title.text = text
        }
    }

    private abstract inner class DisplaySubMenuAction(name:String, val menu:Panel): Action(name) {
        override fun execute(source: Button?) {
            source?.let{
                update()
                displaySubMenu(it, menu)
            }
        }

        /**
         * Called directly before the sub menu is displayed
         */
        abstract fun update()
    }

    private fun displaySubMenu(parent:Panel, subMenu:Panel){
        //for now we're just gonna go overtop of parent
        //translate to the right side of the parent
        subMenu.localTranslation = parent.worldTranslation.add(Vector3f(parent.size.x,0f,0f))
        popupState.showPopup(subMenu)
    }
}

class InteractMenuEvent(val pos:Vector3f, val id:EntityId){
    companion object{
        val requestInteractMenu: EventType<InteractMenuEvent> = EventType.create("requestInteractMenu",InteractMenuEvent::class.java)
    }
}