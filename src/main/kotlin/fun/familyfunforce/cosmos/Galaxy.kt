package `fun`.familyfunforce.cosmos

import com.simsilica.mathd.Vec3d
import java.util.*
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Galaxy is the map data containing all star systems.
 */
data class Galaxy(val name:String, val radius:Double, val systems: List<System>)

/**
 * A system is a node within a galaxy that contains all orbital bodies around a single
 * central body.
 * @param name Name of the star system
 * @param position Galactic position of this star system
 * @param orbitals Collection of orbital bodies that orbit the central star of this system
 */
data class System(val name:String,val position:Vec3d,val orbitals:List<Orbital>)

data class Orbital(val name:String,val position:Vec3d,val size:Double)

/**
 * Generates a random circular galaxy with the given number of stars randomly distributed around a central point
 * with their distance weighted by the gravity value and bound by the radius
 */
fun generateGalaxy(name:String, numSystems:Int, radius:Double, gravity:Double): Galaxy{
    val random = Random()
    val g = if(gravity<1.0) 1.0 else gravity
    //for now, we're just going to generate completely randomly, no checks and no weights
    val systems = List(numSystems) {
        generateSystem("System $it", radius, g, random)
    }
    return Galaxy(name, radius, systems)
}

/**
 * Generates a simple system of planets spaced equally out to the radius.
 */
fun generateSystem(name:String, radius:Double, g:Double, random:Random):System{
    val angle = random.nextDouble()*Math.PI*2
    val dist = g.pow(-random.nextDouble())*radius
    println(dist)
    val pos = Vec3d(dist*cos(angle),0.0,dist*sin(angle))
    val numPlanets = 1+random.nextInt(8)
    val spacing = radius/numPlanets
    val planets = List(numPlanets){
        //for now planets distance = number in sequence
        val planetName = "$name ${(it+10).digitToChar(36)}"
        println(planetName)
        Orbital(planetName,Vec3d(1+it*spacing,0.0,0.0), 1.0)
    }
    return System(name, pos, planets)
}