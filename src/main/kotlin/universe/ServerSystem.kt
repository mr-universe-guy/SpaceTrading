package universe

import com.jme3.app.SimpleApplication
import com.jme3.network.ConnectionListener
import com.jme3.network.HostedConnection
import com.jme3.network.Network
import com.jme3.network.Server
import com.jme3.network.serializing.Serializer
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

class ServerSystem: AbstractGameSystem(){
    var tcpPort = 6111
    var udpPort = 6111
    var server:Server? = null

    override fun initialize() {
    }

    override fun terminate() {
    }

    override fun start() {
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
    }

    private fun initSerializables() {
        Serializer.registerClass(TextMessage::class.java)
    }

    override fun update(time: SimTime?) {
        super.update(time)
    }

    override fun stop() {
        if(server?.isRunning == true){
            server!!.close()
        }
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
}