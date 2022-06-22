/**
 * Space Trader App is a 4x/rts game drawing many parallels to classics such as Eve Online, Elite and X-series, while focusing
 * on single player or small server (approx up to 8 players).
 * Design document available at https://docs.google.com/document/d/1dasutcHd1nyoX8vdQg7cWGhgYvk-QtIAebWO0qliPBo/edit
 */
package `fun`.familyfunforce.cosmos

import `fun`.familyfunforce.cosmos.ui.CameraManagerState
import `fun`.familyfunforce.cosmos.ui.OrbitController
import `fun`.familyfunforce.cosmos.ui.registerDefaults
import com.jme3.app.SimpleApplication
import com.jme3.system.AppSettings
import com.jme3.system.JmeContext
import com.simsilica.lemur.GuiGlobals
import com.simsilica.sim.GameLoop
import com.simsilica.sim.GameSystemManager
import io.tlf.jme.jfx.JavaFxUI
import `fun`.familyfunforce.cosmos.loadout.VehicleLoader
import `fun`.familyfunforce.cosmos.ui.UIAudioState
import com.simsilica.es.net.EntitySerializers
import com.simsilica.lemur.style.BaseStyles
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
        //jfx initialization
        JavaFxUI.initialize(this)
        //lemur
        GuiGlobals.initialize(this)
        //BaseStyles.loadGlassStyle()
        //GuiGlobals.getInstance().styles.defaultStyle = BaseStyles.GLASS
        BaseStyles.loadStyleResources("UI/Space.groovy")
        GuiGlobals.getInstance().styles.defaultStyle = "space"
        //Controls
        registerDefaults(GuiGlobals.getInstance().inputMapper)
        //I/O
        //serializer stuff
        EntitySerializers.initialize()
        Serializers.registerClasses()
        assetManager.registerLoader(VehicleLoader::class.java, "ship")
        //Game Systems
        serverManager = GameSystemManager()
        serverLoop = GameLoop(serverManager, SERVER_RATE)
        serverManager.register(GameLoop::class.java, serverLoop)
        //manager.register(SimpleApplication::class.java, this)
        //manager.register(InputMapper::class.java, GuiGlobals.getInstance().inputMapper)

        /*
        if(initSystems){
            //Turn this off to test individual systems
            attachDataSystems()
            attachPhysicsSystems()
            attachVisualSystems()
            attachAiSystems()
        }
         */
    }

    //Cleanly destroy multi threading
    override fun destroy() {
        //check for client and server
        serverManager.get(ClientState::class.java)?.isEnabled=false
        serverManager.get(ServerSystem::class.java)?.stop()
        serverLoop.stop()
        super.destroy()
    }

    fun attachDataSystems(){
        val dataSystem = LocalDataSystem()
        //TODO: Change this database to the release database
        dataSystem.itemData.fromCSV("/TestItemDB.csv")
        serverManager.register(DataSystem::class.java, dataSystem)
    }

    fun attachPhysicsSystems(){
        serverManager.register(LocalPhysicsSystem::class.java, LocalPhysicsSystem())
        serverManager.addSystem(EngineSystem())
    }

    fun attachVisualSystems(){
        stateManager.attach(VisualState())
        //stateManager.attach(CameraState())
        val camManager = CameraManagerState(cam)
        camManager.activeController = OrbitController(5f,50f, 10f)
        stateManager.attach(camManager)
        stateManager.attach(UIAudioState())
    }

    fun attachAiSystems(){
        serverManager.register(
            ActionSystem::class.java,
            ActionSystem()
        )
    }
}