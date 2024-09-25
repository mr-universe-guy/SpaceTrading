package `fun`.familyfunforce.cosmos.systems

import com.simsilica.es.EntityData
import com.simsilica.es.EntitySet
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import `fun`.familyfunforce.cosmos.Decay
import `fun`.familyfunforce.cosmos.Destroyed

class DestructionSystem: AbstractGameSystem() {
    private lateinit var data: EntityData
    private lateinit var destroyed: EntitySet
    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        destroyed = data.getEntities(Destroyed::class.java)
    }

    override fun terminate() {
        destroyed.release()
    }

    override fun update(time: SimTime) {
        destroyed.applyChanges()
        val t = time.time
        destroyed.addedEntities.forEach {
            println("$it has been destroyed.")
            //TODO: Replace this with logic on turning the destroyed entity into wreckage
            it.set(Decay(t, 0.0))
        }
    }
}