import com.jme3.system.AppSettings
import com.simsilica.mathd.Vec3d
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
        loop.start()
    }
}

fun main(){
    val app = AsteroidDemo()
    val settings = AppSettings(true)
    settings.setResolution(1280,720)
    app.isShowSettings = false
    app.start()
}