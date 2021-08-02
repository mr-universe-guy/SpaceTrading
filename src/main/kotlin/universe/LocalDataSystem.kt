package universe

import com.simsilica.es.EntityData
import com.simsilica.es.base.DefaultEntityData
import com.simsilica.sim.AbstractGameSystem

/**
 * Creates local entity data for single player games
 */
class LocalDataSystem: AbstractGameSystem(), DataSystem {
    private val physData = DefaultEntityData()

    override fun initialize() {

    }

    override fun terminate() {
        physData.close()
    }

    override fun getPhysicsData(): EntityData {
        return physData
    }

    override fun getMarketData(): EntityData {
        TODO("Not yet implemented")
    }
}