/**
 * Space Trader App is a 4x/rts game drawing many parallels to classics such as Eve Online, Elite and X-series, while focusing
 * on single player or small server (approx up to 8 players).
 * Design document available at https://docs.google.com/document/d/1dasutcHd1nyoX8vdQg7cWGhgYvk-QtIAebWO0qliPBo/edit
 */
package universe

import com.jme3.app.SimpleApplication
import com.simsilica.lemur.GuiGlobals
import com.simsilica.sim.GameLoop
import com.simsilica.sim.GameSystemManager
import io.tlf.jme.jfx.JavaFxUI

open class SpaceTraderApp(private val initSystems:Boolean): SimpleApplication(null){
    lateinit var manager: GameSystemManager
    lateinit var loop: GameLoop

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
        stateManager.attach(CameraState())
    }

    fun attachAiSystems(){
        manager.register(ActionSystem::class.java, ActionSystem())
    }
}

fun main(){
    println("Space trading app")
    SpaceTraderApp(true).start()
}