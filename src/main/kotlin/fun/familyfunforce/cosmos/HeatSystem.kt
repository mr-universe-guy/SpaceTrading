package `fun`.familyfunforce.cosmos

import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.EntitySet
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

class HeatSystem: AbstractGameSystem(){
    private lateinit var data: EntityData
    private lateinit var heatChanges: EntitySet
    private lateinit var heatEntities: EntitySet
    private val heatChangeMap: MutableMap<EntityId, Int> = mutableMapOf()
    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        heatChanges = data.getEntities(HeatChange::class.java, TargetId::class.java)
        heatEntities = data.getEntities(HeatLimit::class.java, Heat::class.java, Overheated::class.java)
    }

    override fun update(time: SimTime?) {
        if(heatChanges.applyChanges()){
            heatChanges.addedEntities.forEach {
                val targetId = it.get(TargetId::class.java).targetId
                val targetHeatChange = heatChangeMap[targetId] ?: 0
                heatChangeMap[targetId] = (targetHeatChange + it.get(HeatChange::class.java).heat)
            }
        }
        val heatIterator = heatChangeMap.iterator()
        while(heatIterator.hasNext()){
            val it = heatIterator.next()
            val curHeat = heatEntities.getEntity(it.key)?.get(Heat::class.java)?.heat ?: 0
            val totalHeat = (curHeat + it.value).coerceAtLeast(0)
            data.setComponent(it.key, Heat(totalHeat))
            heatIterator.remove()
        }
        if(heatEntities.applyChanges()){
            heatEntities.changedEntities.forEach {
                val limit = it.get(HeatLimit::class.java).limit
                val curHeat = it.get(Heat::class.java).heat
                val isOverheated = it.get(Overheated::class.java).overheat
                if(isOverheated){
                    if(curHeat == 0){
                        it.set(Overheated(false))
                    }
                } else if(curHeat > limit){
                    it.set(Overheated(true))
                }
            }
        }
    }

    override fun terminate() {
        heatChanges.release()
        heatEntities.release()
        heatChangeMap.clear()
    }
}