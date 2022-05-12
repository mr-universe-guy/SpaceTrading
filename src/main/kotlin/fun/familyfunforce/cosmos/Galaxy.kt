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
data class System(val name:String,val id:Int,val position:Vec3d,val orbitals:List<Orbital>){
    fun updateOrbitals(curTime:Long) {orbitals.forEach{it.updatePositions(curTime)}}
}

/**
 * An object that orbits another object in a System
 * @param argument The angle of periapsis
 * @param period The duration of an orbital period in millis(for now, time unit may change!)
 */
data class Orbital(val name:String, val distance:Double, val argument:Double, val period:Long, val size:Double,
                   val parent: Orbital?, val children:List<Orbital>){
    constructor(name:String, distance: Double, argument: Double, period:Long, size: Double, parent: Orbital?) :
            this(name, distance, argument, period, size, parent, emptyList())
    constructor(name:String, distance: Double, argument: Double, period:Long, size: Double) :
            this(name, distance, argument, period, size, null, emptyList())
    var localPos: Vec3d = Vec3d(0.0,0.0,0.0)
    var globalPos: Vec3d = Vec3d(0.0,0.0,0.0)

    /**
     * This will compute this orbitals position as well as all child orbitals positions
     */
    fun updatePositions(curTime:Long){
        //compute angle and distance and convert it to a 3d translation in local space first
        //TODO:do we NEED to do a modulus here? I'm tempted to say no
        val timeOfYear = curTime%period
        //TODO: The following is a cheat and only works on circular orbits
        val ma = timeOfYear.toDouble()/period.toDouble()
        val angle = ma*Math.PI*2
        //point on circle angle*distance
        localPos.x = distance* cos(angle)
        localPos.z = distance* sin(angle)
        globalPos = (parent?.globalPos ?: Vec3d(0.0,0.0,0.0)).add(localPos)
        children.forEach{it.updatePositions(curTime)}
    }
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
        generateSystem("System $it", it, radius, g, random)
    }
    return Galaxy(name, radius, systems)
}

/**
 * Generates a simple system of planets spaced equally out to the radius.
 */
fun generateSystem(name:String, id:Int, radius:Double, g:Double, random:Random):System{
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
        Orbital(planetName,1+it*spacing,0.0, 60000, 1.0)
    }
    return System(name, id, pos, planets)
}