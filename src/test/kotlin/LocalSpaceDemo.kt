import com.simsilica.es.WatchedEntity
import com.simsilica.mathd.Vec3d
import universe.*

/**
 * A demo of local space interaction. This demo should spawn several asteroids, a station and some destructable targets
 * to test the local space physics systems and combat systems.
 */

class LocalSpaceDemo: SpaceTraderApp(false) {
    private lateinit var watch: WatchedEntity
    override fun simpleInitApp() {
        super.simpleInitApp()
        println("Starting local space demo")
        val dataSystem = LocalDataSystem()
        //create a list of test items purely for this demo
        dataSystem.getItemDatabase().append(listOf(
            Item("ORE", "Ore", 1.0),
            Item("EN", "Energy", 0.1))
        )
        manager.register(DataSystem::class.java, dataSystem)
        manager.register(LocalPhysicsSystem::class.java, LocalPhysicsSystem())
        manager.addSystem(EngineSystem())
        //start the game stuff
        loop.start()
        //spawn a single entity to watch its position change
        val data = manager.get(DataSystem::class.java).getPhysicsData()
        val id = data.createEntity()
        data.setComponents(id,
            Name("Test Ship"),
            Position(Vec3d(0.0,0.0,0.0)), Mass(1.0),
            Velocity(Vec3d(0.0,0.0,10.0)),
            CargoHold(10.0),
            Cargo(arrayOf(ItemStack("ORE", 9), ItemStack("EN", 10))),
            Engine(100.0, 10.0),
            EngineDriver(Vec3d(0.0,0.0,-1.0))
        )
        println("Cargo Data: %s".format(data.getComponent(id, Cargo::class.java)))
        watch = data.watchEntity(id, Velocity::class.java)
    }

    override fun simpleUpdate(tpf: Float) {
        if(watch.applyChanges()) {
            println("Vel: %s".format(watch.get(Velocity::class.java).velocity))
        }
    }
}

fun main(){
    val demo = LocalSpaceDemo()
    demo.start()
}