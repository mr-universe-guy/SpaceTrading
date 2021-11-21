package universe

import com.jme3.app.SimpleApplication
import com.jme3.network.Network
import com.jme3.network.NetworkClient
import com.simsilica.sim.AbstractGameSystem

class ClientSystem: AbstractGameSystem() {
    lateinit var client: NetworkClient
    var tcpPort = 6111
    var udpPort = 6111
    var dest = "localhost"

    override fun initialize() {
        val name = (getSystem(SimpleApplication::class.java) as SpaceTraderApp).appProperties.getProperty("name")
        client = Network.createClient(name, 0)
    }

    override fun start() {
        client.connectToServer(dest,tcpPort,udpPort)
        client.start()
    }

    override fun terminate() {

    }

    override fun stop() {
        if(client.isStarted){
            client.close()
        }
    }
}