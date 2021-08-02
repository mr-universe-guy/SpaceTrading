package universe

import com.simsilica.es.EntityComponent
import com.simsilica.mathd.Vec3d

/**
 * An entities display name
 */
class Name(val name:String): EntityComponent

/**
 * The grid-local 3d position of an entity
 */
class GridPosition(val position:Vec3d): EntityComponent

/**
 * The entities current velocity in m/s
 */
class Velocity(val velocity:Vec3d): EntityComponent

/**
 * The mass of an entity in kg
 */
class Mass(val mass:Double): EntityComponent