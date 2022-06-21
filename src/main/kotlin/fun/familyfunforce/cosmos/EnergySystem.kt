package `fun`.familyfunforce.cosmos

import com.simsilica.es.Entity
import com.simsilica.es.EntityContainer
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime

class EnergySystem: AbstractGameSystem() {
    private lateinit var data: EntityData
    private lateinit var gridContainer: GridContainer
    override fun initialize() {
        data = getSystem(DataSystem::class.java).entityData
        gridContainer = GridContainer((data))
        gridContainer.start()
    }

    override fun update(time: SimTime) {
        gridContainer.updateGrids(time)
    }

    override fun terminate() {
        gridContainer.stop()
    }

    private enum class GridState{
        STABLE,
        CHARGING,
        OVERCHARGED
    }

    fun getGridFromId(id: EntityId): EnergyGrid?{
        return gridContainer.getObject(id)
    }

    inner class EnergyGrid(val id: EntityId, var grid: EnergyGridInfo, private var curEnergy: Long){
        private var state = evaluateState()
        private var nextCycle: Long? = null

        /**
         * Request an amount of energy
         * @param value The amount of energy to consume, or negative to recharge
         */
        fun requestCharge(value: Long): Boolean{
            //we do not have enough energy, simply fail for now
            if(value > 0 && value > curEnergy) return false
            //either value is negative and therefore charging, or we have enough energy to supply the request
            curEnergy -= value
            data.setComponent(id, Energy(curEnergy))
            //evaluate energy grid state
            evaluateState()
            return true
        }

        private fun evaluateState(): GridState{
            val max = grid.maxEnergy
            state = when{
                    curEnergy == max -> {
                        nextCycle = null
                        GridState.STABLE
                    }
                    curEnergy > max -> GridState.OVERCHARGED
                    else -> GridState.CHARGING
                }
            return state
        }

        fun update(time: SimTime){
            when(state){
                GridState.STABLE -> return //grid is stable
                GridState.OVERCHARGED -> {
                    //grid has been overcharged, discard excess energy
                    curEnergy = grid.maxEnergy
                    //evaluate state back to stable
                    state = evaluateState()
                    return
                }
                GridState.CHARGING -> {
                    //If the next cycle has not started, start one now
                    if(nextCycle == null) {
                        nextCycle = time.getFutureTime(grid.cycleTime)
                        return
                    }
                    //wait until cycle timer
                    if(time.time < nextCycle!!) return
                    //perform a charge. Grid can over-charge itself exactly once
                    requestCharge(-grid.recharge)
                    nextCycle = null
                    evaluateState()
                }
            }
        }
    }

    private inner class GridContainer(data: EntityData): EntityContainer<EnergyGrid>(data, EnergyGridInfo::class.java){
        override fun addObject(e: Entity): EnergyGrid {
            val curEn = data.getComponent(e.id, Energy::class.java)?.curEnergy ?: 0
            return EnergyGrid(e.id, (e.get(EnergyGridInfo::class.java)), curEn)
        }

        override fun updateObject(grid: EnergyGrid, e: Entity) {
            grid.grid = e.get(EnergyGridInfo::class.java)
        }

        override fun removeObject(grid: EnergyGrid, e: Entity) {}

        fun updateGrids(time:SimTime){
            update()
            array.forEach {
                it.update(time)
            }
        }
    }
}