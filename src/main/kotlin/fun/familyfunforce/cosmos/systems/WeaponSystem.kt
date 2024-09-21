package `fun`.familyfunforce.cosmos.systems

import com.simsilica.es.Entity
import com.simsilica.es.EntityData
import com.simsilica.es.EntitySet
import com.simsilica.es.Filters
import com.simsilica.es.common.Decay
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import `fun`.familyfunforce.cosmos.*

/**
 * A single system to handle default weapons
 */
class WeaponSystem: AbstractGameSystem() {
    private lateinit var server: ServerSystem
    private lateinit var data:EntityData
    private lateinit var lasers:EntitySet

    override fun initialize() {
        server = getSystem(ServerSystem::class.java)
        data = getSystem(DataSystem::class.java).entityData
        lasers = data.getEntities(
            Filters.fieldEquals(Activated::class.java, "active", true),
            LaserFocus::class.java,
            Activated::class.java,
            Parent::class.java
        )
    }

    override fun terminate() {
        lasers.release()
    }

    override fun update(time: SimTime?) {
        if(lasers.applyChanges()){
            lasers.forEach { fireLaser(it, time!!) }
        }
    }

    private fun fireLaser(it: Entity, time:SimTime) {
        //first ensure the parent has a target, if not cancel this
        val parentId = it.get(Parent::class.java).parentId
        val targetId = data.getComponent(parentId, TargetId::class.java)?.targetId
        if(targetId == null){
            it.set(EquipmentPower(false))
            println("Weapon ${it.id} has no target, disabling")
            return
        }
        println("Firing ma lazor $it")
        //The attack should last long enough that clients can see it, so we can share attack information
        data.setComponents(data.createEntity(),
            Parent(parentId),
            Attack(10, 10, 0),
            TargetId(targetId),
            Decay(time.toSimTime(1.0)),
            VisualAsset("Laser")
        )
        it.set(ActivationConsumed(true))
    }
}