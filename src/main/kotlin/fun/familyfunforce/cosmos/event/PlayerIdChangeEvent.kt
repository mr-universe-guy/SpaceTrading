package `fun`.familyfunforce.cosmos.event

import com.simsilica.es.EntityId
import com.simsilica.event.EventType

/**
 * Event to handle the player entity id changing
 */
class PlayerIdChangeEvent(val playerId:EntityId) {
    companion object {
        /**
         * Signals that a player entity has been created for the first time
         * name: PlayerIdCreated
         */
        val playerIdCreated: EventType<PlayerIdChangeEvent> = EventType.create("PlayerIdCreated",PlayerIdChangeEvent::class.java)

        /**
         * Signals that a player entity has changed
         * name: PlayerIdChanged
         */
        val playerIdChanged: EventType<PlayerIdChangeEvent> = EventType.create("PlayerIdChanged",PlayerIdChangeEvent::class.java)
    }
}