package `fun`.familyfunforce.cosmos.ui

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.WatchedEntity
import com.simsilica.event.EventBus
import com.simsilica.event.EventType
import com.simsilica.lemur.core.VersionedReference
import `fun`.familyfunforce.cosmos.systems.ClientDataState
import `fun`.familyfunforce.cosmos.PlayerIdState
import `fun`.familyfunforce.cosmos.TargetId

/**
 * manages entities the player is focusing or targetting and sends events to alert ui elements of changes
 */
class PlayerFocusState: BaseAppState(){
    init{
        EventBus.addListener(this, EntityFocusEvent.entityFocusRequest)
    }
    private lateinit var data:EntityData
    var focusedId: EntityId? = null
    var targetId: EntityId? = null
    private lateinit var playerId : VersionedReference<EntityId?>
    private var playerEntity:WatchedEntity? = null

    private fun entityFocusRequest(evt : EntityFocusEvent){
        setFocus(evt.id)
    }

    private fun setFocus(targetId:EntityId?){
        //notify current focus it is being lost
        focusedId?.let { EventBus.publish(EntityFocusEvent.entityFocusChanged, EntityFocusEvent(it, false)) }
        //assign and publish the gained event
        focusedId = targetId
        targetId?.let{ EventBus.publish(EntityFocusEvent.entityFocusChanged, EntityFocusEvent(it, true)) }
    }

    fun setPlayerId(id: EntityId){
        playerEntity = data.watchEntity(id, TargetId::class.java)
    }

    override fun update(tpf: Float) {
        if(playerId.update()){
            val pid = playerId.get() ?: return
            setPlayerId(pid)
        }
        if(playerEntity?.applyChanges() != true){return}
        val targetId = playerEntity!!.get(TargetId::class.java)
        //clear old target
        this.targetId?.let { EventBus.publish(TargetingEvent.targetChanged, TargetingEvent(this.targetId, false));}
        //acquire new target
        this.targetId = targetId?.targetId
        targetId?.let { EventBus.publish(TargetingEvent.targetChanged, TargetingEvent(it.targetId, true)); this.targetId = it.targetId }
    }

    override fun initialize(app: Application?) {
        playerId = getState(PlayerIdState::class.java).watchPlayerId()
        data = getState(ClientDataState::class.java).entityData
    }

    override fun cleanup(app: Application?) {
    }

    override fun onEnable() {

    }

    override fun onDisable() {
    }
}

data class EntityFocusEvent(val id:EntityId?, val focused:Boolean){
    companion object{
        val entityFocusRequest: EventType<EntityFocusEvent> = EventType.create("entityFocusRequest", EntityFocusEvent::class.java)
        val entityFocusChanged: EventType<EntityFocusEvent> = EventType.create("entityFocusChanged", EntityFocusEvent::class.java)
    }
}

data class TargetingEvent(val id:EntityId?, val acquired:Boolean){
    companion object{
        val targetChanged: EventType<TargetingEvent> = EventType.create("targetChanged", TargetingEvent::class.java)
    }
}

