import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.WatchedEntity
import com.simsilica.mathd.Quatd
import com.simsilica.mathd.Vec3d
import universe.*

/**
 * A demo of local space interaction. This demo should spawn several asteroids, a station and some destructible targets
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
        //game systems
        manager.register(DataSystem::class.java, dataSystem)
        manager.register(LocalPhysicsSystem::class.java, LocalPhysicsSystem())
        manager.addSystem(EngineSystem())
        //appstates
        stateManager.attach(VisualState())
        stateManager.attach(CameraState())
        //start the game stuff
        loop.start()
        //spawn a single entity to watch its position change
        val data = manager.get(DataSystem::class.java).getPhysicsData()
        val id = spawnShip(data, "Test Ship", Vec3d(0.0,0.0,0.0), Vec3d(0.0,0.0,1.0))
        println("Cargo Data: %s".format(data.getComponent(id, Cargo::class.java)))
        watch = data.watchEntity(id, Velocity::class.java)
        //spawn a couple more ships for reference
        for(i in 1..10){
            val rot = Math.random()*Math.PI*2
            val dist = 10+Math.random()*10
            val pos = Quatd().fromAngles(0.0, rot,0.0).mult(Vec3d(0.0,0.0,dist))
            val dir = Vec3d(Math.random()-0.5, Math.random()-0.5, Math.random()-0.5)
            spawnShip(data, "Ship $i", pos, dir)
        }
    }

    private var isTarget = false
    override fun simpleUpdate(tpf: Float) {
        if(watch.applyChanges()) {
            //println("Vel: %s".format(watch.get(Velocity::class.java).velocity))
            if(!isTarget) stateManager.getState(CameraState::class.java).setTarget(watch.id)
        }
    }

    private fun spawnShip(data:EntityData, name:String, position:Vec3d, direction: Vec3d): EntityId{
        val id = data.createEntity()
        data.setComponents(id,
            Name(name),
            Position(position),
            Mass(1.0),
            Velocity(Vec3d(0.0,0.0,0.0)),
            CargoHold(10.0),
            Cargo(arrayOf(ItemStack("ORE", 9), ItemStack("EN", 10))),
            Engine(100.0, 10.0),
            EngineDriver(direction),
            VisualAsset("TestShip/Insurgent.gltf")
        )
        return id
    }
}

fun main(){
    val demo = LocalSpaceDemo()
    demo.start()
}