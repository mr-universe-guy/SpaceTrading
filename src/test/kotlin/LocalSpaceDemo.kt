import com.jme3.app.StatsAppState
import com.simsilica.es.EntityData
import com.simsilica.es.EntityId
import com.simsilica.es.EntitySet
import com.simsilica.mathd.Quatd
import com.simsilica.mathd.Vec3d
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import universe.*

/**
 * A demo of local space interaction. This demo should spawn several asteroids, a station and some destructible targets
 * to test the local space physics systems and combat systems.
 */

class LocalSpaceDemo: SpaceTraderApp(false) {

    private val actionSys = ActionSystem()

    override fun simpleInitApp() {
        super.simpleInitApp()
        println("Starting local space demo")
        val dataSystem = LocalDataSystem()
        dataSystem.getItemDatabase().fromCSV("/TestItemDB.csv")
        //create a list of test items purely for this demo
        println(dataSystem.getItemDatabase())
        //game systems
        manager.register(DataSystem::class.java, dataSystem)
        manager.register(LocalPhysicsSystem::class.java, LocalPhysicsSystem())
        manager.addSystem(EngineSystem())
        //AI should be last
        manager.register(ActionSystem::class.java, actionSys)
        //app states
        //stateManager.attach(VisualState())
        //stateManager.attach(CameraState())
        stateManager.attach(StatsAppState())
        //stateManager.attach(FlightUIState())
        //add this loop listener to test stuff
        manager.addSystem(LoopListener())
        //start the game stuff
        loop.start()
    }

    private fun spawnShip(data:EntityData, name:String, position:Vec3d): EntityId{
        val id = data.createEntity()
        data.setComponents(id,
            Name(name),
            Position(position),
            Mass(1.0),
            Velocity(Vec3d(0.0,0.0,0.0)),
            CargoHold(10.0),
            Cargo(arrayOf(ItemStack("ORE", 9), ItemStack("EN", 10))),
            Engine(100.0, 10.0),
            EngineDriver(Vec3d(0.0,0.0,0.0)),
            VisualAsset("TestShip/Insurgent.gltf")
        )
        return id
    }

    private fun randomVec3d(scalar: Double): Vec3d{
        return Vec3d((Math.random()-0.5)*2*scalar, (Math.random()-0.5)*2*scalar, (Math.random()-0.5)*2*scalar)
    }

    private inner class LoopListener: AbstractGameSystem(){
        private lateinit var data: EntityData
        private lateinit var testEntities: EntitySet

        override fun initialize() {
            data = getSystem(DataSystem::class.java).getPhysicsData()
            testEntities = data.getEntities(ActionInfo::class.java)
            for(i in 1..100){
                val rot = Math.random()*Math.PI*2
                val dist = 10+Math.random()*10
                val pos = Quatd().fromAngles(0.0, rot,0.0).mult(Vec3d(0.0,0.0,dist))
                val id = spawnShip(data, "Ship $i", pos)
                //assign a random move action
                val dest = randomVec3d(100.0)
                actionSys.setAction(id, MoveAction(dest))
            }
        }

        override fun update(time: SimTime?) {
            if(testEntities.applyChanges()){
                testEntities.changedEntities.forEach {
                    val info = it.get(ActionInfo::class.java)
                    if(info.status != ActionStatus.COMPLETE) return
                    println("${it.id} has finished it's action ${it.get(ActionInfo::class.java).text}.")
                    val nextAction = MoveAction(randomVec3d(100.0))
                    actionSys.setAction(it.id, nextAction)
                    println("${it.id} beginning new action $nextAction")
                }
            }
        }

        override fun terminate() {

        }

    }
}

fun main(){
    val demo = LocalSpaceDemo()
    demo.isShowSettings = false
    demo.start()
}