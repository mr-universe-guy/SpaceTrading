package `fun`.familyfunforce.cosmos.event

import com.simsilica.event.EventType
import `fun`.familyfunforce.cosmos.Orbital

class StellarTravelEvent(val target: Orbital){
    companion object{
        val SetDestination: EventType<StellarTravelEvent> = EventType.create("SetDestination", StellarTravelEvent::class.java)
    }
}