package universe

import com.jme3.app.SimpleApplication
import com.jme3.network.Client
import com.jme3.network.Network
import com.simsilica.sim.AbstractGameSystem

class ClientSystem: AbstractGameSystem() {
    var tcpPort = 6111
    var udpPort = 6111
    var client: Client? = null

    override fun initialize() {

    }

    override fun start() {
        val name = (getSystem(SimpleApplication::class.java) as SpaceTraderApp).appProperties.getProperty("name")
        client = Network.connectToServer(name,0,"localhost",tcpPort,udpPort)
        client!!.addMessageListener { source, m ->
            if(m is TextMessage){
                println(m.message)
            }
        }
        client!!.start()
    }

    override fun terminate() {

    }

    override fun stop() {
        if(client?.isStarted == true){
            client!!.close()
        }
    }
}