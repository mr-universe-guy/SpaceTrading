package `fun`.familyfunforce.cosmos

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.network.*
import com.jme3.network.serializing.Serializer
import com.simsilica.es.client.EntityDataClientService
import com.simsilica.sim.AbstractGameSystem

class ServerSystem: AbstractGameSystem(){
    private val listeners = mutableListOf<ServerStatusListener>()
    var tcpPort = 6111
    var udpPort = 6111
    val server:Server = Network.createServer(SpaceTraderApp.appProperties.getProperty("name"), 0, udpPort, tcpPort)
    var serverStatus = ServerStatus.CLOSED
        private set(value) {
            field = value
            listeners.forEach { it.statusChanged(value) }
        }

    override fun initialize() {

    }

    override fun terminate() {
    }

    fun startServer() {
        serverStatus = ServerStatus.INITIALIZING
        initSerializables()
        server.addMessageListener { _, m ->
            if(m is TextMessage){
                server.broadcast(m)
            }
        }
        server.addConnectionListener(ConnListener())
        server.start()
        serverStatus = if(server.isRunning) ServerStatus.RUNNING else ServerStatus.CLOSED
    }

    fun stopServer(){
        if(server.isRunning){
            server.close()
        }
        serverStatus = ServerStatus.CLOSED
    }

    private fun initSerializables() {
        Serializer.registerClass(TextMessage::class.java)
    }

    override fun stop() {
        stopServer()
    }

    private class ConnListener: ConnectionListener{
        override fun connectionAdded(server: Server?, conn: HostedConnection?) {
            println("Connection established: $conn")
            server!!.broadcast(TextMessage("New Player: $conn"))
        }

        override fun connectionRemoved(server: Server?, conn: HostedConnection?) {
            println("$conn disconnected")
        }
    }

    fun addServerStatusListener(l: ServerStatusListener){
        listeners.add(l)
    }

    fun removeServerStatusListener(l: ServerStatusListener){
        listeners.remove(l)
    }

    enum class ServerStatus{
        CLOSED,
        INITIALIZING,
        RUNNING
    }

    fun interface ServerStatusListener{
        fun statusChanged(status:ServerStatus)
    }
}

class ClientState: BaseAppState() {
    val client: NetworkClient
    init{
        val name = SpaceTraderApp.appProperties.getProperty("name")
        client = Network.createClient(name, 0)
        //add services before client starts
        val services = client.services
        services.addService(EntityDataClientService(MessageConnection.CHANNEL_DEFAULT_RELIABLE))
    }
    private val tcpPort = 6111
    private val udpPort = 6111

    fun connectTo(dest:String){
        client.connectToServer(dest,tcpPort,udpPort)
        client.start()
    }

    override fun initialize(app: Application?) {

    }

    override fun cleanup(app: Application?) {
    }

    override fun onEnable() {
    }

    override fun onDisable() {
        if(client.isStarted){
            client.close()
        }
    }
}