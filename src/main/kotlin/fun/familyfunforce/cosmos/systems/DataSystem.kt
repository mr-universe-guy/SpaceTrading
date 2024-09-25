package `fun`.familyfunforce.cosmos.systems

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.network.Client
import com.jme3.network.MessageConnection
import com.jme3.network.Server
import com.simsilica.es.EntityData
import com.simsilica.es.base.DefaultEntityData
import com.simsilica.es.client.EntityDataClientService
import com.simsilica.es.server.EntityDataHostedService
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import `fun`.familyfunforce.cosmos.ItemDatabase

/**
 * A game system that stores game and market entity data
 */
interface DataSystem {
    val entityData:EntityData
    val itemData: ItemDatabase
}

/**
 * Creates local entity data for single player games
 */
class LocalDataSystem: AbstractGameSystem(), DataSystem {
    override val entityData = DefaultEntityData()
    override val itemData = ItemDatabase()

    override fun initialize() {
    }

    override fun terminate() {
        entityData.close()
    }
}

class HostDataSystem(private val server:Server, override val itemData: ItemDatabase): AbstractGameSystem(), DataSystem {
    override val entityData = DefaultEntityData()
//    override val itemData = ItemDatabase()
    //we want to do all of this immediately and not as a part of the game loop
    private val service = EntityDataHostedService(MessageConnection.CHANNEL_DEFAULT_RELIABLE, entityData)
    init{
        server.services.addService(service)
    }

    override fun update(time: SimTime?) {
        service.sendUpdates()
    }

    override fun initialize() {}

    //close our data
    override fun terminate() {
        entityData.close()
    }
}

/**
 *
 */
class ClientDataState(private val client:Client): BaseAppState(){
    val entityData: EntityData = client.services.getService(EntityDataClientService::class.java).entityData!!
    //TODO: FTP the item database to ensure server is authoritative
    var itemData = ItemDatabase()

    override fun initialize(app: Application?) {}

    override fun cleanup(app: Application?) {
        entityData.close()
    }

    override fun onEnable() {}

    override fun onDisable() {}
}