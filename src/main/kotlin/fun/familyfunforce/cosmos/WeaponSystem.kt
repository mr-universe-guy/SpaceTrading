package `fun`.familyfunforce.cosmos

import com.simsilica.es.Entity
import com.simsilica.es.EntityData
import com.simsilica.es.EntitySet
import com.simsilica.es.Filters
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

/**
 * A single system to handle default weapons
 */
class WeaponSystem: AbstractGameSystem() {
    private lateinit var data:EntityData
    private lateinit var lasers:EntitySet
    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        lasers = data.getEntities(Filters.fieldEquals(Activated::class.java, "active", true),LaserFocus::class.java,
            Activated::class.java, )
    }

    override fun terminate() {
        lasers.release()
    }

    override fun update(time: SimTime?) {
        if(lasers.applyChanges()){
            lasers.forEach { fireLaser(it) }
        }
    }

    private fun fireLaser(it: Entity) {
        println("Firing ma lazor $it")
    }
}