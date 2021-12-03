package `fun`.familyfunforce.cosmos

import com.simsilica.es.EntityData

/**
 * A game system that stores game and market entity data
 */
interface DataSystem {
    fun getPhysicsData():EntityData
    fun getItemDatabase():ItemDatabase
}