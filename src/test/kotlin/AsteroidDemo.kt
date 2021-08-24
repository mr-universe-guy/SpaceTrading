import com.jme3.system.AppSettings
import com.simsilica.es.EntityId
import com.simsilica.es.WatchedEntity
import com.simsilica.mathd.Vec3d
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import universe.*

/*
 * A simple demo for interacting with a randomly generated asteroid field
 */

class AsteroidDemo: SpaceTraderApp(false){
    override fun simpleInitApp() {
        println("Starting Asteroid Demo")
        super.simpleInitApp()
        //data
        attachDataSystems()
        //use general physics
        attachPhysicsSystems()
        manager.register(EnergySystem::class.java, EnergySystem())
        //use general visuals
        attachVisualSystems()
        //ai
        attachAiSystems()
        println("Default systems loaded")
        //lock in game systems

        //spawn a player ship and a couple of asteroids
        val data = manager.get(DataSystem::class.java).getPhysicsData()
        val playerId = spawnTestShip(data, "Player", Vec3d(0.0,0.0,0.0))
        println("Player Spawned")
        //register player to systems that care
        enqueue{
            stateManager.getState(CameraState::class.java).setTarget(playerId)
            println("Camera target set")
        }
        //spawn asteroids
        val asteroidID = spawnTestAsteroid(data, Vec3d(1.0,0.0,50.0))
        println("Asteroid spawned")
        //order player to orbit asteroid for now
        manager.enqueue {
            //manager.get(ActionSystem::class.java).setAction(playerId, OrbitAction(asteroidID, 20.0))
        }
        manager.addSystem(LoopListener(playerId))
        loop.start()
    }
}

class LoopListener(private val playerId: EntityId): AbstractGameSystem(){
    private lateinit var player: WatchedEntity
    override fun initialize() {
        val data = getSystem(DataSystem::class.java).getPhysicsData()
        player = data.watchEntity(playerId, Energy::class.java)
    }

    override fun update(time: SimTime?) {
        if(player.applyChanges()){
            println(player.get(Energy::class.java))
        }
    }

    override fun terminate() {

    }
}

fun main(){
    val app = AsteroidDemo()
    val settings = AppSettings(true)
    settings.setResolution(1280,720)
    app.isShowSettings = false
    app.start()
}