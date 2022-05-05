import `fun`.familyfunforce.cosmos.*
import `fun`.familyfunforce.cosmos.event.PlayerIdChangeEvent
import `fun`.familyfunforce.cosmos.ui.CameraManagerState
import `fun`.familyfunforce.cosmos.ui.OrbitController
import `fun`.familyfunforce.cosmos.ui.SystemMapState
import com.jme3.system.AppSettings
import com.simsilica.event.EventBus
import com.simsilica.mathd.Vec3d
import kotlin.random.Random
import kotlin.random.asJavaRandom

class SystemInteractionDemo: SpaceTraderApp(false) {
    override fun simpleInitApp() {
        println("Starting system interaction demo")
        super.simpleInitApp()
        //phys
        val data = LocalDataSystem()
        manager.register(DataSystem::class.java, data)
        //vis
        stateManager.attach(VisualState())
        //camera
        val cms = CameraManagerState(cam)
        val camCont=OrbitController(5f,100f)
        cms.activeController=camCont
        stateManager.attach(cms)
        val mapState = SystemMapState()
        stateManager.attach(mapState)
        //generate a random system
        val system = generateSystem("Test System", 1, 100.0, 5.0, Random.asJavaRandom())
        //spawn a player controllable object
        val pid = data.getPhysicsData().createEntity()
        data.getPhysicsData().setComponents(pid,
                StellarObject(0.5),
                StellarPosition(system.id, Vec3d(5.0,0.0,5.0))
            )
        stateManager.attach(object:FirstFrameState(){
            override fun onFirstFrame() {
                EventBus.publish(PlayerIdChangeEvent.PlayerIdCreated, PlayerIdChangeEvent(pid))
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