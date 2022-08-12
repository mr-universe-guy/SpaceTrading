//please stop telling me about all the empty constructors that only get called via reflection
@file:Suppress("unused")
package `fun`.familyfunforce.cosmos

import com.jme3.network.serializing.Serializer
import com.jme3.network.serializing.serializers.FieldSerializer
import com.simsilica.es.ComponentFilter
import com.simsilica.es.EntityComponent
import com.simsilica.es.EntityId
import com.simsilica.mathd.Vec3d

object Serializers{
    private val classes = arrayOf(
        Name::class.java,
        Position::class.java,
        Velocity::class.java,
        VisualAsset::class.java,
        Vec3d::class.java,
        ObjectCategory::class.java,
        Parent::class.java,
        EquipmentPower::class.java,
        CycleTimer::class.java,
        ParentFilter::class.java,
        TargetLock::class.java,
        TargetTrack::class.java,
        Energy::class.java
    )

    fun serializeComponents(){
        classes.forEach {
            Serializer.registerClass(it, FieldSerializer())
            println("$it registered")
        }
    }
}

/**
 * An entities display name
 */
@com.jme3.network.serializing.Serializable
data class Name(var name:String): EntityComponent{
    constructor() : this("")
}

/**
 * The grid-local 3d position of an entity
 */
@com.jme3.network.serializing.Serializable
data class Position(var position:Vec3d): EntityComponent{
    constructor() : this(Vec3d(0.0,0.0,0.0))
}

/**
 * The entities current velocity in m/s
 */
@com.jme3.network.serializing.Serializable
data class Velocity(var velocity:Vec3d): EntityComponent{
    constructor(x: Double, y: Double, z: Double) : this(Vec3d(x,y,z))
    constructor() : this(Vec3d(0.0,0.0,0.0))
}

/**
 * The mass of an entity in kg
 */
data class Mass(var mass:Double): EntityComponent

/**
 * The position relative to a star system referenced by the star systems id
 */
data class StellarPosition(var systemId:Int, var position:Vec3d): EntityComponent

/**
 * An object that can be interacted with within a star system, such as fleets, asteroids, anomalies, etc.
 */
data class StellarObject(var radius:Double): EntityComponent

/**
 * Size of an entities inventory in cubic meters
 */
data class CargoHold(var volume:Double): EntityComponent

data class Cargo(var items: Array<ItemStack>): EntityComponent {
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
data class EngineDriver(var direction:Vec3d): EntityComponent

/**
 * Engine will provide thrust until the maximum speed is met
 */
data class Engine(var maxSpeed:Double, var thrust:Double): EntityComponent

/**
 * For now just the text to the asset. In the future this will likely have to store more info.
 */
@com.jme3.network.serializing.Serializable
data class VisualAsset(var asset:String): EntityComponent{
    constructor() : this("")
}

/**
 * Read published varues for an entities current action
 */
data class ActionInfo(var text: String, var status: ActionStatus): EntityComponent

/**
 * Drag to be applied to moving entities to create more natural maximum speeds and accelerations
 * @param dragCoefficient Should be a number 0.0 < 1.0
 */
data class Drag(var dragCoefficient: Double): EntityComponent

/**
 * The current energy an entity has available
 */
@com.jme3.network.serializing.Serializable
data class Energy(var curEnergy: Long): EntityComponent{
    constructor():this(0)
}

/**
 * The maximum energy an entity can have
 * @param maxEnergy the maximum energy this grid can contain, excess energy will be discarded
 * @param recharge the amount of energy that is re-charged per cycle
 * @param cycleTime the amount of time, in seconds to complete one recharge cycle
 */
data class EnergyGridInfo(var maxEnergy: Long, var recharge: Long, var cycleTime: Double): EntityComponent

/**
 * Categories of objects that can spawn. Used to determine hud elements and sorting player side
 */
@com.jme3.network.serializing.Serializable
enum class Category{
    SHIP, ASTEROID
}

/**
 * Component to store the Category of a given entity. Used to sort HUD elements and similar
 */
@com.jme3.network.serializing.Serializable
data class ObjectCategory(var category: Category): EntityComponent{
    constructor() : this(Category.SHIP)
}

/**
 * The stats to determine how a target can be tracked by weapons systems
 */
data class Sensors(var range: Double): EntityComponent

/**
 * Store target locks as a component so other systems can break locks, etc
 */
@com.jme3.network.serializing.Serializable
data class TargetLock(var targetId: EntityId): EntityComponent{
    constructor() : this(EntityId.NULL_ID)
}

/**
 * Store info turrets need to shoot at their target
 */
@com.jme3.network.serializing.Serializable
data class TargetTrack(var distance:Double, var angVel:Double): EntityComponent{
    constructor(): this(-1.0, 0.0)
}

/**
 * Store the time the next cycle is supposed to occur as well as the amount of time in seconds a cycle lasts
 */
@com.jme3.network.serializing.Serializable
data class CycleTimer(var nextCycle: Long, var duration:Double): EntityComponent{
    constructor() : this(0,0.0)
}

/**
 * Simple active/not active component
 */
@com.jme3.network.serializing.Serializable
data class EquipmentPower(var active:Boolean): EntityComponent{
    constructor() : this(false)
}

/**
 * Marks an entity as being activated.
 */
data class Activated(var active:Boolean): EntityComponent{
    constructor():this(false)
}

/**
 * Component holding the EquipmentId of a given piece of equipment
 */
data class EquipmentAsset(var equipmentId:String): EntityComponent

/**
 * Identifies an entity ID that acts as the parent to this entity
 */
@com.jme3.network.serializing.Serializable
data class Parent(var parentId:EntityId): EntityComponent{
    constructor() : this(EntityId.NULL_ID)
}

/**
 * Finds only Parent components that have a parentId matching the specified ID
 * @param parentId The EntityId to match, or Null to always return false
 */
@com.jme3.network.serializing.Serializable
class ParentFilter(private var parentId:EntityId?): ComponentFilter<Parent> {
    constructor() : this(EntityId.NULL_ID)
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

//lets do the most basic weapon
/**
 * @param focalLength the distance at which the laser does maximum damage
 * @param focalDepth the range from the focal length that the laser will do damage based on the global laser falloff
 * TODO: laser falloff???
 */
@com.jme3.network.serializing.Serializable
@kotlinx.serialization.Serializable
data class LaserFocus(var focalLength:Double, var focalDepth:Double): EntityComponent {
    constructor(): this(0.0,0.0)
}
