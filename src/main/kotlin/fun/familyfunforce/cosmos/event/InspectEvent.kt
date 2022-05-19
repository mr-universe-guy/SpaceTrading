package `fun`.familyfunforce.cosmos.event

import `fun`.familyfunforce.cosmos.ui.Inspectable
import com.simsilica.event.EventType

/**
 * Used when focus is brought to an inspect-able item in the game. This will notify the inspector window to update with
 * the new items info
 */
class InspectEvent(val inspectable: Inspectable?) {
    companion object{
        /**
         * Request inspection of the inspectable object
         */
        val InspectionRequest : EventType<InspectEvent> = EventType.create(InspectEvent::class.java)
    }
}