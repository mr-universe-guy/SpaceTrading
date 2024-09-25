/**
 * Space Trader App is a 4x/rts game drawing many parallels to classics such as Eve Online, Elite and X-series, while focusing
 * on single player or small server (approx up to 8 players).
 * Design document available at https://docs.google.com/document/d/1dasutcHd1nyoX8vdQg7cWGhgYvk-QtIAebWO0qliPBo/edit
 */
package `fun`.familyfunforce.cosmos

import com.jme3.app.SimpleApplication
import com.jme3.system.AppSettings
import com.jme3.system.JmeContext
import com.simsilica.es.net.EntitySerializers
import com.simsilica.lemur.GuiGlobals
import com.simsilica.lemur.OptionPanelState
import com.simsilica.lemur.anim.AnimationState
import com.simsilica.lemur.style.BaseStyles
import com.simsilica.sim.GameLoop
import com.simsilica.sim.GameSystemManager
import `fun`.familyfunforce.cosmos.loadout.VehicleLoader
import `fun`.familyfunforce.cosmos.systems.*
import `fun`.familyfunforce.cosmos.ui.*
import java.util.*

/**
 * Target hertz for the server to update physics and the like
 */
const val SERVER_HZ = 10

/**
 * Server rate in nano-seconds, determined by SERVER_HZ
 */
const val SERVER_RATE: Long = 1_000_000_000L/SERVER_HZ

fun main(){
    println("Space trading app")
    val app = SpaceTraderApp(true)
    app.start()
}

open class SpaceTraderApp(private val initSystems:Boolean): SimpleApplication(null){
    companion object{
        val appProperties : Properties = Properties()
    }

    lateinit var serverManager: GameSystemManager
    lateinit var serverLoop: GameLoop
    init{
        isPauseOnLostFocus=false
    }

    override fun start(contextType: JmeContext.Type?, waitFor: Boolean) {
        appProperties.load(this::class.java.getResourceAsStream("version.properties"))
        val appSettings = settings ?: AppSettings(true)
        appSettings.title = appProperties.getProperty("name")+" : "+appProperties.getProperty("version")
        super.start(contextType, waitFor)
    }

    override fun simpleInitApp() {
        //lemur
        GuiGlobals.initialize(this)
        //BaseStyles.loadGlassStyle()
        //GuiGlobals.getInstance().styles.defaultStyle = BaseStyles.GLASS
        BaseStyles.loadStyleResources("UI/Space.groovy")
        GuiGlobals.getInstance().styles.defaultStyle = "space"
        //I/O
        //serializer stuff
        EntitySerializers.initialize()
        Serializers.serializeComponents()
        Actions.serializeActions()
        Inventories.serializeInventories()
        assetManager.registerLoader(VehicleLoader::class.java, "ship")
        //Game Systems
        serverManager = GameSystemManager()
        serverLoop = GameLoop(serverManager, SERVER_RATE)
        serverManager.register(GameLoop::class.java, serverLoop)
        serverManager.addSystem(DecaySystem())
    }

    //Cleanly destroy multi threading
    override fun destroy() {
        //check for client and server
        serverManager.get(ClientState::class.java)?.isEnabled=false
        serverManager.get(ServerSystem::class.java)?.stop()
        serverLoop.stop()
        super.destroy()
    }

    fun attachServerSystems(): ServerSystem{
        val server = ServerSystem()
        serverManager.register(ServerSystem::class.java, server)
        val dataSystem = HostDataSystem(server.server, createTestItemDatabase())
        serverManager.register(DataSystem::class.java, dataSystem)
        serverManager.register(InventorySystem::class.java, InventorySystem())
        serverManager.register(LocalPhysicsSystem::class.java, LocalPhysicsSystem())
        serverManager.addSystem(DragSystem())
        serverManager.addSystem(EngineSystem())
        serverManager.register(EnergySystem::class.java, EnergySystem())
        serverManager.register(SensorSystem::class.java, SensorSystem())
        serverManager.addSystem(PoweredEquipmentSystem())
        serverManager.register(ActionSystem::class.java, ActionSystem())
        serverManager.addSystem(WeaponSystem())
        serverManager.addSystem(CombatSystem())
        serverManager.addSystem(MiningSystem())
        serverManager.addSystem(HeatSystem())
//        serverManager.addSystem(DestructionSystem())
        return server
    }

    fun attachPhysicsSystems(){
        serverManager.register(LocalPhysicsSystem::class.java, LocalPhysicsSystem())
        serverManager.addSystem(EngineSystem())
    }

    fun attachVisualSystems(){
        //Controls
        registerDefaults(GuiGlobals.getInstance().inputMapper)
        //states
        stateManager.attach(OptionPanelState())
        stateManager.attach(PlayerIdState())
        stateManager.attach(PlayerFocusState())
        stateManager.attach(AnimationState())
        stateManager.attach(VisualState())
        val cameraManagerState = CameraManagerState(cam)
        cameraManagerState.activeController = OrbitController(5f,100f, 10f)
        stateManager.attach(cameraManagerState)
        stateManager.attach(LocalMapState())
        stateManager.attach(ShipHudState())
        stateManager.attach(UIAudioState())
        stateManager.attach(ClientActionEventResponder())
        stateManager.attach(InteractionMenuState())
    }

    fun attachAiSystems(){
        serverManager.register(
            ActionSystem::class.java,
            ActionSystem()
        )
    }
}