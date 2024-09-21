package `fun`.familyfunforce.cosmos.systems

import com.simsilica.es.Entity
import com.simsilica.es.EntityData
import com.simsilica.es.EntitySet
import com.simsilica.es.Filters
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import `fun`.familyfunforce.cosmos.*

//Basically the attack system but for mining minerals
//As well as damaging the rock, successful mining attacks extract minerals from the deposits
class MiningSystem: AbstractGameSystem() {
    private lateinit var data: EntityData
    private lateinit var miners: EntitySet
    private lateinit var inventorySystem: InventorySystem
    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        miners = data.getEntities(
            Filters.fieldEquals(
                Activated::class.java, "active", true),
                MiningPower::class.java,
                Activated::class.java,
                Parent::class.java
        )
        inventorySystem = getSystem(InventorySystem::class.java)
    }

    override fun terminate() {
        miners.release()
    }

    override fun update(time: SimTime) {
        if(miners.applyChanges()){
             miners.forEach { createMiningAttack(it, time) }
        }
    }

    private fun createMiningAttack(it: Entity, time: SimTime) {
        val parentId = it.get(Parent::class.java).parentId
        //TODO: Verifying targets should be it's own system!!!
        val targetId = data.getComponent(parentId, TargetId::class.java)?.targetId
        if(targetId == null){
            it.set(EquipmentPower(false))
            println("Mining Equipment ${it.id} has no target, disabling")
            return
        }
        //ensure target is an asteroid
        val mineral = data.getComponent(targetId, Mineral::class.java)
        if(mineral == null){
            it.set(EquipmentPower(false))
            println("Target $targetId is not a mineral deposit, disabling")
            return
        }
        val miningPower = it.get(MiningPower::class.java)
        val inventoryStatus = inventorySystem.requestAddItem(parentId.id, mineral.type, miningPower.power.toLong())
        if(inventoryStatus != TransactionStatus.SUCCESS){
            it.set(EquipmentPower(false))
            println("Failed to transfer minerals to inventory :$inventoryStatus")
            return
        }
        println("Successfully mined ${miningPower.power} ${mineral.type}")
        it.set(ActivationConsumed(true))
    }
}