package `fun`.familyfunforce.cosmos.ui

import `fun`.familyfunforce.cosmos.ClientSystem
import `fun`.familyfunforce.cosmos.SpaceTraderApp
import `fun`.familyfunforce.cosmos.TextMessage
import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.app.state.BaseAppState
import com.jme3.input.KeyInput
import com.jme3.math.Vector3f
import com.jme3.network.Client
import com.jme3.network.Message
import com.jme3.network.MessageListener
import com.simsilica.lemur.ListBox
import com.simsilica.lemur.Panel
import com.simsilica.lemur.TextField
import com.simsilica.lemur.component.BorderLayout
import com.simsilica.lemur.component.TextEntryComponent
import com.simsilica.lemur.core.GuiControl
import com.simsilica.lemur.event.KeyAction
import com.simsilica.lemur.event.KeyActionListener

/**
 * App State to handle chat ui, alerts and message history
 */
class ChatState: BaseAppState(){
    private var client: Client? = null
    private val chatBox = ChatBox(300f,200f)
    private val chatBoxListener = ChatBoxListener { text -> sendChatMessage(text) }
    private val messageListener = MessageListener<Client> { s, m -> receiveChatMessage(s, m) }

    override fun initialize(app: Application?) {
        app as SimpleApplication
        app.guiNode.attachChild(chatBox)
        chatBox.setLocalTranslation(0f,300f,0f)
        chatBox.addChatListener(chatBoxListener)
    }

    override fun cleanup(app: Application?) {
        chatBox.removeFromParent()
    }

    private fun sendChatMessage(text:String){
        val msg = TextMessage(text)
        client!!.send(msg)
    }

    private fun receiveChatMessage(src:Client, msg:Message){
        chatBox.receiveChatMsg(msg as TextMessage)
    }

    override fun onEnable() {
        //attach self to client
        val app = application as SpaceTraderApp
        client = app.manager.get(ClientSystem::class.java)!!.client
        client!!.addMessageListener(messageListener, TextMessage::class.java)
    }

    override fun onDisable() {
        client?.removeMessageListener(messageListener)
        client = null
    }

    private class ChatBox(width:Float,height:Float): Panel(width,height), KeyActionListener{
        private val chatBoxListeners = arrayListOf<ChatBoxListener>()
        //private val chatDisplay = Container(BoxLayout(Axis.Y, FillMode.None))
        private val chatDisplay = ListBox<String>()
        private val chatEntry = TextField("")
        init{
            chatEntry.preferredSize = Vector3f(2000f, 50f, 1f)
            chatEntry.actionMap[KeyAction(KeyInput.KEY_RETURN)] = this
            val layout = BorderLayout()
            getControl(GuiControl::class.java).setLayout(layout)
            layout.addChild(chatDisplay, BorderLayout.Position.North)
            layout.addChild(chatEntry, BorderLayout.Position.South)
        }

        fun addChatListener(l:ChatBoxListener){chatBoxListeners.add(l)}
        fun removeChatListener(l:ChatBoxListener){chatBoxListeners.remove(l)}

        fun receiveChatMsg(msg:TextMessage){
            chatDisplay.model.add(msg.message)
            chatDisplay.slider.model.value = chatDisplay.slider.model.minimum
        }

        override fun keyAction(source: TextEntryComponent, key: KeyAction?) {
            //clear source text and execute the stored action
            //sanitize text
            val msg = source.documentModel.text.trim()
            if(msg.isEmpty() || msg.isBlank()) return
            //publish to listeners
            chatBoxListeners.forEach{ l -> l.onChatSubmit(msg)}
            source.documentModel.text=""
        }
    }

    private fun interface ChatBoxListener{
        fun onChatSubmit(text:String)
    }
}