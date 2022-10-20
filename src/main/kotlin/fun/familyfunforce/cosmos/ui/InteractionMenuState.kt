package `fun`.familyfunforce.cosmos.ui

import `fun`.familyfunforce.cosmos.DataSystem
import `fun`.familyfunforce.cosmos.Name
import `fun`.familyfunforce.cosmos.Position
import `fun`.familyfunforce.cosmos.SpaceTraderApp
import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.math.Vector2f
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.WatchedEntity
import com.simsilica.event.EventBus
import com.simsilica.event.EventType
import com.simsilica.lemur.*
import com.simsilica.lemur.component.BorderLayout
import com.simsilica.lemur.component.BoxLayout
import com.simsilica.lemur.event.PopupState

/**
 * Manages an interaction menu, it's hidden or visible status,
 * it's current focus and any active/deactive actions
 */
class InteractionMenuState: BaseAppState() {
    private lateinit var data:EntityData
    private lateinit var popupState:PopupState
    private var interactTarget:WatchedEntity? = null
    private object NavMenuAction: Action("Navigation") {
        override fun execute(source: Button?) {
            TODO("Not yet implemented")
        }
    }
    private object InteractMenuAction: Action("Interaction"){
        override fun execute(source: Button?) {
            TODO("Not yet implemented")
        }
    }
    private object CommMenuAction: Action("Communication"){
        override fun execute(source: Button?) {
            TODO("Not yet implemented")
        }
    }
    //private val interactPanel = OptionPanel("", navMenuAction, interactMenuAction, commMenuAction);
    private val interactPanel = InteractMenu("", NavMenuAction, InteractMenuAction, CommMenuAction)

    override fun initialize(app: Application?) {
        data = (app as SpaceTraderApp).serverManager.get(DataSystem::class.java).entityData
        popupState = getState(PopupState::class.java)
    }

    override fun update(tpf: Float) {
        if(interactTarget?.applyChanges() != true) return
        val tgt = interactTarget!!
        //verify actions
        val cName:Name? = tgt.get(Name::class.java)
        interactPanel.setTitle(cName?.name ?:"Unknown")
        val cPos = tgt.get(Position::class.java)
        if(cPos != null){

        }
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
            popupState.showPopup(interactPanel)
            //optionState.show(interactPanel)
        }
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
}

class InteractMenuEvent(val pos:Vector2f, val id:EntityId){
    companion object{
        val requestInteractMenu: EventType<InteractMenuEvent> = EventType.create("requestInteractMenu",InteractMenuEvent::class.java)
    }
}