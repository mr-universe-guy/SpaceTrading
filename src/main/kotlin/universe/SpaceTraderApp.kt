/**
 * Space Trader App is a 4x/rts game drawing many parallels to classics such as Eve Online, Elite and X-series, while focusing
 * on single player or small server (approx up to 8 players).
 * Design document available at https://docs.google.com/document/d/1dasutcHd1nyoX8vdQg7cWGhgYvk-QtIAebWO0qliPBo/edit
 */
package universe

import com.jme3.app.SimpleApplication
import com.jme3.system.AppSettings
import com.jme3.system.JmeContext
import com.simsilica.lemur.GuiGlobals
import com.simsilica.lemur.input.InputMapper
import com.simsilica.sim.GameLoop
import com.simsilica.sim.GameSystemManager
import io.tlf.jme.jfx.JavaFxUI
import universe.ui.CameraManagerState
import universe.ui.OrbitController
import universe.ui.registerDefaults
import java.util.*

fun main(){
    println("Space trading app")
    val app = SpaceTraderApp(true)
    app.start()
}

open class SpaceTraderApp(private val initSystems:Boolean): SimpleApplication(null){
    val appProperties : Properties = Properties()
    init {
        appProperties.load(this::class.java.getResourceAsStream("version.properties"))
    }
    lateinit var manager: GameSystemManager
    lateinit var loop: GameLoop

    override fun start(contextType: JmeContext.Type?, waitFor: Boolean) {
        val appSettings = settings ?: AppSettings(true)
        appSettings.title = appProperties.getProperty("name")+" : "+appProperties.getProperty("version")
        super.start(contextType, waitFor)
    }

    override fun simpleInitApp() {
        //jfx initialization
        JavaFxUI.initialize(this)
        //lemur
        GuiGlobals.initialize(this)
        //Controls
        registerDefaults(GuiGlobals.getInstance().inputMapper)
        //I/O
        assetManager.registerLoader(VehicleLoader::class.java, "ship")
        //Game Systems
        manager = GameSystemManager()
        manager.register(SimpleApplication::class.java, this)
        manager.register(InputMapper::class.java, GuiGlobals.getInstance().inputMapper)
        loop = GameLoop(manager)
        if(initSystems){
            //Turn this off to test individual systems
            attachDataSystems()
            attachPhysicsSystems()
            attachVisualSystems()
            attachAiSystems()
        }
    }

    //Cleanly destroy multi threading
    override fun destroy() {
        //check for client and server
        manager.get(ClientSystem::class.java)?.stop()
        manager.get(ServerSystem::class.java)?.stop()
        loop.stop()
        super.destroy()
    }

    fun attachDataSystems(){
        val dataSystem = LocalDataSystem()
        //TODO: Change this database to the release database
        dataSystem.getItemDatabase().fromCSV("/TestItemDB.csv")
        manager.register(DataSystem::class.java, dataSystem)
    }

    fun attachPhysicsSystems(){
        manager.register(LocalPhysicsSystem::class.java, LocalPhysicsSystem())
        manager.addSystem(EngineSystem())
    }

    fun attachVisualSystems(){
        stateManager.attach(VisualState())
        //stateManager.attach(CameraState())
        val camManager = CameraManagerState(cam)
        camManager.activeController = OrbitController(5f,50f)
        stateManager.attach(camManager)
    }

    fun attachAiSystems(){
        manager.register(ActionSystem::class.java, ActionSystem())
    }
}