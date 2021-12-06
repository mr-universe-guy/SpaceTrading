package `fun`.familyfunforce.cosmos

import com.simsilica.mathd.Vec3d
import java.util.*
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Galaxy is the map data containing all star systems.
 */
class Galaxy(val name:String, val radius:Double, val systems: List<System>) {

}

/**
 * A system is a node within a galaxy that contains all orbital bodies around a single
 * central body.
 */
class System(val name:String,val position: Vec3d){

}

/**
 * Generates a random circular galaxy with the given number of stars randomly distributed around a central point
 * with their distance weighted by the gravity value and bound by the radius
 */
fun generateGalaxy(name:String, numSystems:Int, radius:Double, gravity:Double): Galaxy{
    val random = Random()
    val g = if(gravity<1.0) 1.0 else gravity
    //for now, we're just going to generate completely randomly, no checks and no weights
    val systems = List(numSystems) {
        val angle = random.nextDouble()*Math.PI*2
        val dist = g.pow(-random.nextDouble())*radius
        println(dist)
        val name = "System $it"
        val pos = Vec3d(dist*cos(angle),0.0,dist*sin(angle))
        System(name, pos)
    }
    return Galaxy(name, radius, systems)
}