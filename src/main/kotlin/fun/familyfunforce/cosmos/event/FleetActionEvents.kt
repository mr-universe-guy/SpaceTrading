package `fun`.familyfunforce.cosmos.event

import `fun`.familyfunforce.cosmos.Orbital
import com.simsilica.event.EventType

class StellarTravelEvent(val target: Orbital){
    companion object{
        val SetDestination: EventType<StellarTravelEvent> = EventType.create("SetDestination", StellarTravelEvent::class.java)
    }
}