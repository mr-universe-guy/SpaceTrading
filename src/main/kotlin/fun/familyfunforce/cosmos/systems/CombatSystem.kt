package `fun`.familyfunforce.cosmos.systems

import com.simsilica.es.Entity
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.EntitySet
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import `fun`.familyfunforce.cosmos.*

class CombatSystem : AbstractGameSystem() {
    private lateinit var data : EntityData
    private lateinit var attacks : EntitySet
    private lateinit var damagedEntities : EntitySet

    private val attackMap = mutableMapOf<EntityId, Accumulator>()

    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        attacks = data.getEntities(Attack::class.java, TargetId::class.java)
        damagedEntities = data.getEntities(Damage::class.java, HealthPoints::class.java)
    }

    override fun terminate() {
        attacks.release()
        damagedEntities.release()
    }

    override fun update(time: SimTime?) {
        if(!attacks.applyChanges()) return
        // only process new attacks, let old ones decay
        attacks.addedEntities.forEach {
            val atk = it!!.get(Attack::class.java)
            val targetId = it.get(TargetId::class.java).targetId
            val accumulator = attackMap[targetId]
            if(accumulator == null){
                attackMap[targetId] = Accumulator(atk.armorDamage, atk.shieldDamage, atk.miningDamage)
            } else{
                accumulator.add(atk.armorDamage, atk.shieldDamage, atk.miningDamage)
            }
        }
        //apply all damages at the end of frame
        val iterator = attackMap.iterator()
        while(iterator.hasNext()){
            val entry = iterator.next()
            println("Entity Id ${entry.key} has received ${entry.value} damage")
            //update the targets damage value
            val tgt = data.getEntity(entry.key, Damage::class.java)
            val dmg = tgt.get(Damage::class.java) ?: Damage(0,0,0)
            val sum = entry.value.sumDamages(dmg)
            data.setComponent(entry.key, sum)
            println("Entity Id ${entry.key} has received $sum total damage")
            //cleanup
            iterator.remove()
        }
        damagedEntities.applyChanges()
        damagedEntities.addedEntities.forEach { evalEntityDestruction(it) }
        damagedEntities.changedEntities.forEach { evalEntityDestruction(it) }
    }

    private fun evalEntityDestruction(entity: Entity){
        val hp = entity.get(HealthPoints::class.java).armor
        val damage = entity.get(Damage::class.java).armorDamage
        if(damage > hp){
            entity.set(Destroyed(true))
        }
    }

    data class Accumulator(var armor:Int, var shield:Int, var mining:Int){
        fun add(a:Int, s:Int, m:Int){
            armor += a
            shield += s
            mining += m
        }

        fun sumDamages(damageA: Damage): Damage {
            return Damage(damageA.armorDamage+armor, damageA.shieldDamage+shield, damageA.miningDamage+mining)
        }
    }
}