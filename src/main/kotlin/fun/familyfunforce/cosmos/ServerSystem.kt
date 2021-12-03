package `fun`.familyfunforce.cosmos

import com.jme3.app.SimpleApplication
import com.jme3.network.ConnectionListener
import com.jme3.network.HostedConnection
import com.jme3.network.Network
import com.jme3.network.Server
import com.jme3.network.serializing.Serializer
import com.simsilica.sim.AbstractGameSystem

class ServerSystem: AbstractGameSystem(){
    private val listeners = mutableListOf<ServerStatusListener>()
    var tcpPort = 6111
    var udpPort = 6111
    var server:Server? = null
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
        val name = (getSystem(SimpleApplication::class.java) as SpaceTraderApp).appProperties.getProperty("name")
        server = Network.createServer(name,0, udpPort, tcpPort)
        initSerializables()
        server!!.addMessageListener { source, m ->
            if(m is TextMessage){
                server!!.broadcast(m)
            }
        }
        server!!.addConnectionListener(ConnListener())
        server!!.start()
        serverStatus = if(server!!.isRunning) ServerStatus.RUNNING else ServerStatus.CLOSED
    }

    fun stopServer(){
        if(server?.isRunning == true){
            server!!.close()
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