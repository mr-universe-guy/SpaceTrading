import `fun`.familyfunforce.cosmos.FirstFrameState
import `fun`.familyfunforce.cosmos.SpaceTraderApp
import `fun`.familyfunforce.cosmos.generateSystem
import `fun`.familyfunforce.cosmos.ui.CameraManagerState
import `fun`.familyfunforce.cosmos.ui.OrbitController
import `fun`.familyfunforce.cosmos.ui.SystemMapState
import com.jme3.system.AppSettings
import kotlin.random.Random
import kotlin.random.asJavaRandom

class SystemInteractionDemo: SpaceTraderApp(false) {
    override fun simpleInitApp() {
        println("Starting system interaction demo")
        super.simpleInitApp()
        //camera
        val cms = CameraManagerState(cam)
        val camCont=OrbitController(5f,100f)
        cms.activeController=camCont
        stateManager.attach(cms)
        val mapState = SystemMapState()
        stateManager.attach(mapState)
        //generate a random system
        val system = generateSystem("Test System", 100.0, 5.0, Random.asJavaRandom())
        stateManager.attach(object:FirstFrameState(){
            override fun onFirstFrame() {
                mapState.system=system
            }
        })
    }
}

fun main(){
    val app = SystemInteractionDemo()
    val settings = AppSettings(true)
    settings.setResolution(1280,720)
    app.setSettings(settings)
    app.isShowSettings=false
    app.start()
}