package universe

import com.simsilica.es.EntityComponent
import com.simsilica.mathd.Vec3d

/**
 * An entities display name
 */
data class Name(val name:String): EntityComponent

/**
 * The grid-local 3d position of an entity
 */
data class Position(val position:Vec3d): EntityComponent

/**
 * The entities current velocity in m/s
 */
data class Velocity(val velocity:Vec3d): EntityComponent

/**
 * The mass of an entity in kg
 */
data class Mass(val mass:Double): EntityComponent

/**
 * Size of an entities inventory in cubic meters
 */
data class CargoHold(val volume:Double): EntityComponent

data class Cargo(val items: Array<ItemStack>): EntityComponent {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cargo

        if (!items.contentEquals(other.items)) return false

        return true
    }

    override fun hashCode(): Int {
        return items.contentHashCode()
    }
}

/**
 * Apply thrust in direction until throttle percentage of max speed is met
 */
data class EngineDriver(val direction:Vec3d): EntityComponent

/**
 * Engine will provide thrust until the maximum speed is met
 */
data class Engine(val maxSpeed:Double, val thrust:Double): EntityComponent

/**
 * For now just the text to the asset. In the future this will likely have to store more info.
 */
data class VisualAsset(val asset:String): EntityComponent