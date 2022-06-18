package `fun`.familyfunforce.cosmos

import com.simsilica.es.ComponentFilter
import com.simsilica.es.EntityComponent
import com.simsilica.es.EntityId
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
data class Velocity(val velocity:Vec3d): EntityComponent{
    constructor(x: Double, y: Double, z: Double) : this(Vec3d(x,y,z))
}

/**
 * The mass of an entity in kg
 */
data class Mass(val mass:Double): EntityComponent

/**
 * The position relative to a star system referenced by the star systems id
 */
data class StellarPosition(val systemId:Int, val position:Vec3d): EntityComponent

/**
 * An object that can be interacted with within a star system, such as fleets, asteroids, anomalies, etc.
 */
data class StellarObject(val radius:Double): EntityComponent

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

/**
 * Read published values for an entities current action
 */
data class ActionInfo(val text: String, val status: ActionStatus): EntityComponent

/**
 * Drag to be applied to moving entities to create more natural maximum speeds and accelerations
 * @param dragCoefficient Should be a number 0.0 < 1.0
 */
data class Drag(val dragCoefficient: Double): EntityComponent

/**
 * The current energy an entity has available
 */
data class Energy(val curEnergy: Long): EntityComponent

/**
 * The maximum energy an entity can have
 * @param maxEnergy the maximum energy this grid can contain, excess energy will be discarded
 * @param recharge the amount of energy that is re-charged per cycle
 * @param cycleTime the amount of time, in seconds to complete one recharge cycle
 */
data class EnergyGridInfo(val maxEnergy: Long, val recharge: Long, val cycleTime: Double): EntityComponent

/**
 * Categories of objects that can spawn. Used to determine hud elements and sorting player side
 */
enum class Category{
    SHIP, ASTEROID
}

/**
 * Component to store the Category of a given entity. Used to sort HUD elements and similar
 */
data class ObjectCategory(val category: Category): EntityComponent

/**
 * The stats to determine how a target can be tracked by weapons systems
 */
data class Sensors(val range: Double): EntityComponent

/**
 * Store target locks as a component so other systems can break locks, etc
 */
data class TargetLock(val targetId: EntityId): EntityComponent

/**
 * Store the time the next cycle is supposed to occur as well as the amount of time in seconds a cycle lasts
 */
data class CycleTimer(val nextCycle: Long, val duration:Double): EntityComponent

data class AttackStrength(val atk: Int): EntityComponent

/**
 * Simple active/not active component
 */
data class Activate(val active:Boolean): EntityComponent

/**
 * Component holding the EquipmentId of a given piece of equipment
 */
data class EquipmentAsset(val equipmentId:String): EntityComponent

/**
 * Identifies an entity ID that acts as the parent to this entity
 */
data class Parent(val parentId:EntityId): EntityComponent

/**
 * Finds only Parent components that have a parentId matching the specified ID
 * @param parentId The EntityId to match, or Null to always return false
 */
class ParentFilter(private val parentId:EntityId?): ComponentFilter<Parent> {
    override fun getComponentType(): Class<Parent> {
        return Parent::class.java
    }

    override fun evaluate(c: EntityComponent?): Boolean {
        parentId ?: return false
        c ?: return false
        if(c !is Parent) return false
        return c.parentId==parentId
    }
}