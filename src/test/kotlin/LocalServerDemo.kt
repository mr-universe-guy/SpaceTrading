import com.jme3.network.Client
import com.jme3.network.ClientStateListener
import universe.ClientSystem
import universe.ServerSystem
import universe.SpaceTraderApp
import universe.ui.ChatState

fun main(){
    val app = ServerDemo()
    app.isShowSettings = false
    app.start()
}

class ServerDemo: SpaceTraderApp(false){
    override fun simpleInitApp() {
        println("Starting local-server test")
        super.simpleInitApp()
        manager.register(ServerSystem::class.java, ServerSystem())
        manager.register(ClientSystem::class.java, ClientSystem())
        //init systems
        manager.initialize()
        //register client listeners and states
        manager.get(ClientSystem::class.java).client.addClientStateListener(object : ClientStateListener {
            override fun clientConnected(c: Client?) {
                setClientSystemsEnabled(true)
            }
            override fun clientDisconnected(c: Client?, info: ClientStateListener.DisconnectInfo?) {
                setClientSystemsEnabled(false)
            }
        })
        val chat = ChatState()
        chat.isEnabled = false
        stateManager.attach(chat)
        //start the game system
        manager.start()
    }

    fun setClientSystemsEnabled(enabled: Boolean){
        stateManager.getState(ChatState::class.java).isEnabled = enabled
    }
}