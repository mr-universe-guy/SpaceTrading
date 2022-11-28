package `fun`.familyfunforce.cosmos

import `fun`.familyfunforce.cosmos.event.PlayerIdChangeEvent
import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.simsilica.es.EntityId
import com.simsilica.event.EventBus
import com.simsilica.lemur.core.VersionedHolder
import com.simsilica.lemur.core.VersionedReference

/**
 * Tracks player id changes and makes them available for other states
 */
class PlayerIdState:BaseAppState() {
    //private var playerId:EntityId? = null
    private val playerId = VersionedHolder<EntityId?>()

    override fun initialize(app: Application?) {}

    override fun cleanup(app: Application?) {}

    fun setPlayerId(id:EntityId){
        playerId.updateObject(id)
    }

    fun watchPlayerId():VersionedReference<EntityId?>{
        return playerId.createReference()
    }

    fun onPlayerIdChanged(event:PlayerIdChangeEvent){
        playerId.updateObject(event.playerId)
    }

    fun onPlayerIdCreated(event:PlayerIdChangeEvent){
        playerId.updateObject(event.playerId)
    }

    override fun onEnable() {
        EventBus.addListener(this, PlayerIdChangeEvent.playerIdChanged, PlayerIdChangeEvent.playerIdCreated)
    }

    override fun onDisable() {
        EventBus.removeListener(this, PlayerIdChangeEvent.playerIdChanged, PlayerIdChangeEvent.playerIdCreated)
    }
}