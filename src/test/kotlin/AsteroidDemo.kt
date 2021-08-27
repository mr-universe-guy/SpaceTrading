import com.jme3.system.AppSettings
import com.simsilica.es.EntityId
import com.simsilica.es.WatchedEntity
import com.simsilica.lemur.GuiGlobals
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
        val dataSystem = LocalDataSystem()
        //TODO: Change this database to the release database
        dataSystem.getItemDatabase().fromCSV("/TestItemDB.csv")
        manager.register(DataSystem::class.java, dataSystem)
        //Controls
        registerDefaults(GuiGlobals.getInstance().inputMapper)
        //use general physics
        manager.register(LocalPhysicsSystem::class.java, LocalPhysicsSystem())
        manager.addSystem(EngineSystem())
        manager.register(EnergySystem::class.java, EnergySystem())
        //use general visuals
        stateManager.attach(VisualState())
        stateManager.attach(CameraState())
        stateManager.attach(ShipHudState())
        //ai
        manager.register(ActionSystem::class.java, ActionSystem())
        println("Default systems loaded")
        //lock in game systems

        //spawn a player ship and a couple of asteroids
        val data = manager.get(DataSystem::class.java).getPhysicsData()
        val playerId = spawnTestShip(data, "Player", Vec3d(0.0,0.0,0.0))
        println("Player Spawned")
        //register player to systems that care
        enqueue{
            stateManager.getState(CameraState::class.java).setTarget(playerId)
            stateManager.getState(ShipHudState::class.java).playerId = playerId
            println("Camera target set")
        }
        //spawn asteroids
        val asteroidID = spawnTestAsteroid(data, Vec3d(25.0,25.0,75.0))
        spawnTestAsteroid(data, Vec3d(-25.0, -25.0, 75.0))
        spawnTestAsteroid(data, Vec3d(0.0, 0.0, -100.0))
        //order player to orbit asteroid for now
        manager.enqueue {
            manager.get(ActionSystem::class.java).setAction(playerId, OrbitAction(asteroidID, 50.0))
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
    app.setSettings(settings)
    app.isShowSettings = false
    app.start()
}