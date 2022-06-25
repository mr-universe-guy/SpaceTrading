package `fun`.familyfunforce.cosmos

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.network.*
import com.jme3.network.serializing.Serializer
import com.jme3.network.service.rmi.RmiClientService
import com.jme3.network.service.rmi.RmiHostedService
import com.jme3.network.service.rpc.RpcClientService
import com.jme3.network.service.rpc.RpcHostedService
import com.simsilica.es.client.EntityDataClientService
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import java.util.concurrent.TimeUnit

const val NANOS_TO_SECONDS = 1.0/1_000_000_000.0

@com.jme3.network.serializing.Serializable
data class TimerMessage(var serverTime:Long, var lastUpdateTime:Long):AbstractMessage(true){
    constructor():this(0,0)
}

class ServerSystem: AbstractGameSystem(){
    private val listeners = mutableListOf<ServerStatusListener>()
    var tcpPort = 6111
    var udpPort = 6111
    val server:Server = Network.createServer(SpaceTraderApp.appProperties.getProperty("name"), 0, udpPort, tcpPort)
    init{
        val services = server.services
        services.addService(RpcHostedService())
        services.addService(RmiHostedService())
    }
    var serverStatus = ServerStatus.CLOSED
        private set(value) {
            field = value
            listeners.forEach { it.statusChanged(value) }
        }

    override fun initialize() {
    }

    override fun terminate() {
    }

    override fun update(time: SimTime?) {
    }

    fun startServer() {
        serverStatus = ServerStatus.INITIALIZING
        initSerializables()
        server.addMessageListener { _, m ->
            if(m is TextMessage){
                server.broadcast(m)
            }
        }
        server.addMessageListener(TimerListener(), TimerMessage::class.java)
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
        Serializer.registerClasses(TextMessage::class.java, TimerMessage::class.java)
    }

    override fun stop() {
        stopServer()
    }

    fun sendTimer(conn:HostedConnection){
        //val serverTime = java.lang.System.nanoTime()
        val unlocked = manager.stepTime.getUnlockedTime(java.lang.System.nanoTime())
        conn.send(TimerMessage(unlocked, unlocked))
    }

    private inner class ConnListener: ConnectionListener{
        override fun connectionAdded(server: Server?, conn: HostedConnection) {
            println("Connection established: $conn")
            server!!.broadcast(TextMessage("New Player: $conn"))
            sendTimer(conn)
        }

        override fun connectionRemoved(server: Server?, conn: HostedConnection?) {
            println("$conn disconnected")
        }
    }

    private inner class TimerListener: MessageListener<HostedConnection>{
        override fun messageReceived(source: HostedConnection?, m: Message?) {
            sendTimer(source!!)
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
    var lastServerUpdate = Long.MIN_VALUE
    private var lastFrameTime = java.lang.System.nanoTime()
    private var lastPingTime = lastFrameTime
    @Volatile var approxSimTime = lastFrameTime

    /**
     * Time it takes for a message to go one way in millis
     */
    var ping = 0L

    init{
        val name = SpaceTraderApp.appProperties.getProperty("name")
        client = Network.createClient(name, 0)
        //add services before client starts
        val services = client.services
        services.addService(EntityDataClientService(MessageConnection.CHANNEL_DEFAULT_RELIABLE))
        services.addService(RpcClientService())
        services.addService(RmiClientService())
        client.addMessageListener(TimerListener(), TimerMessage::class.java)
    }
    private val tcpPort = 6111
    private val udpPort = 6111

    fun connectTo(dest:String){
        client.connectToServer(dest,tcpPort,udpPort)
        client.start()
    }

    override fun update(tpf: Float) {
        val curTime = java.lang.System.nanoTime()
        val delta = curTime-lastFrameTime
        approxSimTime += delta
        lastFrameTime=curTime
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

    private inner class TimerListener:MessageListener<Client>{
        override fun messageReceived(source: Client?, m: Message?) {
            //we're gonna do some figuring out here
            m as TimerMessage
            lastServerUpdate = m.serverTime
            val curTime = java.lang.System.nanoTime()
            val deltaNanos = curTime-lastPingTime
            val pingNanos = deltaNanos/2
            ping = TimeUnit.NANOSECONDS.toMillis(pingNanos)
            //println("ping:$ping")
            source!!.send(m)
            lastPingTime = curTime
            approxSimTime = lastServerUpdate+pingNanos
        }
    }
}