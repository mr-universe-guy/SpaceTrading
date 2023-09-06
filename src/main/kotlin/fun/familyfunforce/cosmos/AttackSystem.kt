package `fun`.familyfunforce.cosmos

import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.EntitySet
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

class AttackSystem : AbstractGameSystem() {
    private lateinit var data : EntityData
    private lateinit var attacks : EntitySet

    private val attackMap = mutableMapOf<EntityId, Int>()

    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        attacks = data.getEntities(Attack::class.java, TargetId::class.java)
    }

    override fun terminate() {
        attacks.release()
    }

    override fun update(time: SimTime?) {
        if(!attacks.applyChanges()) return
        attacks.forEach {
            val atk = it!!.get(Attack::class.java)
            val targetId = it.get(TargetId::class.java).targetId
            val accum = attackMap[targetId] ?: 0
            attackMap[targetId] = accum+atk.damage
            //we always consume the attack the same frame it was created
            //if we want to share attack info it will need to be somewhere else
            data.removeEntity(it.id)
        }
        //apply all damages at the end of frame
        val iterator = attackMap.iterator()
        while(iterator.hasNext()){
            val entry = iterator.next()
            println("Entity Id ${entry.key} has recieved ${entry.value} damage")
            iterator.remove()
        }
    }
}