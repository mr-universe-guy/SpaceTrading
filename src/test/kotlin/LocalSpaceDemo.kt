import com.jme3.app.state.AppState
import com.simsilica.es.WatchedEntity
import com.simsilica.mathd.Vec3d
import universe.*

/**
 * A demo of local space interaction. This demo should spawn several asteroids, a station and some destructable targets
 * to test the local space physics systems and combat systems.
 */

class LocalSpaceDemo(): SpaceTraderApp(false) {
    lateinit var watch: WatchedEntity
    override fun simpleInitApp() {
        super.simpleInitApp()
        println("Starting local space demo")
        manager.register(DataSystem::class.java, LocalDataSystem())
        manager.addSystem(LocalPhysicsSystem())
        //start the game stuff
        loop.start()
        //spawn a single entity to watch it's position change
        val data = manager.get(DataSystem::class.java).getPhysicsData()
        val id = data.createEntity()
        data.setComponents(id, GridPosition(Vec3d(0.0,0.0,0.0)), Mass(1.0), Velocity(Vec3d(0.0,0.0,1.0)))
        watch = data.watchEntity(id, GridPosition::class.java)
    }

    override fun simpleUpdate(tpf: Float) {
        watch.applyChanges()
        println("Position: %s".format(watch.get(GridPosition::class.java).position))
    }
}

fun main(){
    val demo = LocalSpaceDemo()
    demo.start()
}