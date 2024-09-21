package `fun`.familyfunforce.cosmos.systems

import com.simsilica.es.EntityData
import com.simsilica.es.EntitySet
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import `fun`.familyfunforce.cosmos.Decay
import `fun`.familyfunforce.cosmos.DecayTicks

/**
 * Removes entities after the time or tick decay expires
 */
class DecaySystem: AbstractGameSystem() {
    private lateinit var data:EntityData
    private lateinit var timeDecays:EntitySet
    private lateinit var tickDecays:EntitySet

    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        timeDecays = data.getEntities(Decay::class.java)
        tickDecays = data.getEntities(DecayTicks::class.java)
    }

    override fun update(time: SimTime) {
        val curTime = time.time
        timeDecays.applyChanges()
        timeDecays.filter{it.get(Decay::class.java)!!.end <= curTime}
            .forEach {
                data.removeEntity(it.id)
                println("$it has been removed")
            }

        val curTick = time.frame
        tickDecays.applyChanges()
        tickDecays.filter {it.get(DecayTicks::class.java).endTick<=curTick}
            .forEach {data.removeEntity(it.id); }

    }

    override fun terminate() {
        timeDecays.release()
        tickDecays.release()
    }

    fun decayInSeconds(seconds:Double): Decay {
        val future = manager.stepTime.getFutureTime(seconds)
        return Decay(future, seconds)
    }
}