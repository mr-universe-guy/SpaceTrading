package universe

import com.simsilica.es.EntityData

/**
 * A game system that stores game and market entity data
 */
interface DataSystem {
    fun getPhysicsData():EntityData
}