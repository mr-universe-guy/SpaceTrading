import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.network.Client
import com.jme3.network.ClientStateListener
import com.jme3.system.AppSettings
import com.simsilica.es.EntityId
import com.simsilica.es.WatchedEntity
import com.simsilica.event.EventBus
import com.simsilica.lemur.GuiGlobals
import com.simsilica.mathd.Vec3d
import com.simsilica.sim.AbstractGameSystem
import com.simsilica.sim.SimTime
import `fun`.familyfunforce.cosmos.*
import `fun`.familyfunforce.cosmos.event.PlayerIdChangeEvent
import `fun`.familyfunforce.cosmos.ui.*

/*
 * A simple demo for interacting with a randomly generated asteroid field
 */

class LocalSpaceDemo: SpaceTraderApp(false){
    lateinit var playerId: EntityId
    private lateinit var targetId: EntityId
    override fun simpleInitApp() {
        println("Starting Asteroid Demo")
        super.simpleInitApp()
        //************************  SERVER  ************************
        val server = ServerSystem()
        serverManager.register(ServerSystem::class.java, server)
        //data
        val dataSystem = HostDataSystem(server.server)
        dataSystem.itemData.fromCSV("/TestItemDB.csv")
        serverManager.register(DataSystem::class.java, dataSystem)
        serverManager.addSystem(DecaySystem())
        serverManager.register(LocalPhysicsSystem::class.java, LocalPhysicsSystem())
        serverManager.addSystem(EngineSystem())
        serverManager.register(EnergySystem::class.java, EnergySystem())
        serverManager.register(SensorSystem::class.java, SensorSystem())
        serverManager.addSystem(ActiveEquipmentSystem())
        serverManager.register(ActionSystem::class.java,ActionSystem())
        serverManager.addSystem(WeaponSystem())
        serverManager.addSystem(AttackSystem())

        //***********************   CLIENT  *************************
        //focus
        //serverManager.register(EntityFocusManager::class.java, EntityFocusManager())

        //
        val client = ClientState()
        client.client.addClientStateListener(object:ClientStateListener{
            override fun clientConnected(c: Client?) {
                //connect all states here
                stateManager.attach(ClientDataState(client.client))
                stateManager.attach(PlayerIdState())
                stateManager.attach(PlayerFocusState())
                stateManager.attach(VisualState())
                val cameraManagerState = CameraManagerState(cam)
                cameraManagerState.activeController = OrbitController(5f,100f, 10f)
                stateManager.attach(cameraManagerState)
                stateManager.attach(LocalMapState())
                stateManager.attach(ShipHudState())
                stateManager.attach(UIAudioState())
                stateManager.attach(ClientActionEventResponder())
                stateManager.attach(InteractionMenuState())
                println("Client initialized")
                //register player to systems that care
                val initListener =object: UpdateListener(){
                    override fun onUpdate(tpf: Float) {
                        if(!stateManager.getState(VisualState::class.java).isInitialized) return
                        EventBus.publish(PlayerIdChangeEvent.playerIdCreated, PlayerIdChangeEvent(playerId))
                        stateManager.detach(this)
                    }
                }
                stateManager.attach(initListener)
            }
            override fun clientDisconnected(c: Client?, info: ClientStateListener.DisconnectInfo?) {}
        })
        stateManager.attach(client)
        //
        registerDefaults(GuiGlobals.getInstance().inputMapper)
        //***********************   DEMO    *************************
        //spawn a player ship and a couple of asteroids
        val data = serverManager.get(DataSystem::class.java).entityData
        val loadout = generateTestLoadout()
        playerId = spawnLoadout(data, "Player", Vec3d(0.0,0.0,0.0), loadout)
        println("Player Spawned as id $playerId")
        //spawn something to shoot at
        targetId = spawnLoadout(data, "Target", Vec3d(0.0, -50.0, 0.0), loadout)
        //spawn asteroids
        val asteroidID = spawnTestAsteroid(data, Vec3d(25.0,25.0,75.0))
        spawnTestAsteroid(data, Vec3d(-25.0, -25.0, 75.0))
        spawnTestAsteroid(data, Vec3d(0.0, 0.0, -100.0))
        //order player to orbit asteroid for now
        serverManager.enqueue {
            val actSys = serverManager.get(ActionSystem::class.java)
            actSys.setAction(playerId, OrbitAction(asteroidID, 50.0))
            actSys.setAction(targetId, OrbitAction(asteroidID, 100.0))
        }
        serverManager.addSystem(LoopListener(playerId))
        serverLoop.start()

        //finish client
        server.startServer()
        client.connectTo("localhost")
    }
}

private abstract class UpdateListener: BaseAppState(){
    abstract fun onUpdate(tpf:Float)

    override fun initialize(app: Application?) {}
    override fun cleanup(app: Application?) {}
    override fun onEnable() {}
    override fun onDisable() {}
    override fun update(tpf: Float) {
        onUpdate(tpf)
    }
}

class LoopListener(private val playerId: EntityId): AbstractGameSystem(){
    private lateinit var player: WatchedEntity
    override fun initialize() {
        val data = getSystem(DataSystem::class.java).entityData
        player = data.watchEntity(playerId, Energy::class.java, Position::class.java)
    }

    override fun update(time: SimTime?) {
        //if(player.applyChanges()){
            //println(player.get(Energy::class.java))
        //}
    }

    override fun terminate() {

    }
}

fun main(){
    val app = LocalSpaceDemo()
    val settings = AppSettings(true)
    settings.setResolution(1280,720)
    app.setSettings(settings)
    app.isShowSettings = false
    app.start()
}