import universe.ClientSystem
import universe.ServerSystem
import universe.SpaceTraderApp

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
        manager.initialize()
        manager.start()
        println("Server started successfully")
    }
}