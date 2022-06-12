package `fun`.familyfunforce.cosmos.ui

import com.simsilica.es.EntityId
import com.simsilica.event.EventBus
import com.simsilica.event.EventType

class EntityFocusManager{
    init{
        EventBus.addListener(this, EntityFocusEvent.entityFocusRequest)
    }
    var focusedId: EntityId? = null

    private fun entityFocusRequest(evt : EntityFocusEvent){
        setFocus(evt.id)
    }

    private fun setFocus(targetId:EntityId?){
        //notify current focus it is being lost
        focusedId?.let { EventBus.publish(EntityFocusEvent.entityFocusLost, EntityFocusEvent(it)) }
        //assign and publish the gained event
        focusedId = targetId
        targetId?.let{ EventBus.publish(EntityFocusEvent.entityFocusGained, EntityFocusEvent(it)) }
    }
}

data class EntityFocusEvent(val id:EntityId){
    companion object{
        val entityFocusRequest: EventType<EntityFocusEvent> = EventType.create("entityFocusRequest", EntityFocusEvent::class.java)
        val entityFocusGained: EventType<EntityFocusEvent> = EventType.create("entityFocusGained", EntityFocusEvent::class.java)
        val entityFocusLost: EventType<EntityFocusEvent> = EventType.create("entityFocusLost", EntityFocusEvent::class.java)
    }
}

