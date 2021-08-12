package universe

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.simsilica.es.Entity
import com.simsilica.es.EntityContainer
import com.simsilica.es.EntityData
import com.simsilica.mathd.Vec3d
import io.tlf.jme.jfx.JavaFxUI
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.ListView
import javafx.scene.control.ScrollPane

class FlightUIState: BaseAppState() {
    private lateinit var tgtList: Node
    @FXML
    private lateinit var tgtListPane: ScrollPane
    private val nearbyTgtList = ListView<Target>()
    private lateinit var data: EntityData
    private lateinit var objects: TargetListContainer
    private val frameTime = 1f/30f
    private var timer = 0f

    override fun initialize(_app: Application?) {
        val app = _app as SpaceTraderApp
        data = app.manager.get(DataSystem::class.java).getPhysicsData()
        objects = TargetListContainer(data)
        val loader = FXMLLoader(javaClass.getResource("/UI/TestTargetsList.fxml" ))
        loader.setController(this)
        tgtList = loader.load()
    }

    fun initialize(){
        tgtListPane.content = nearbyTgtList
    }

    override fun cleanup(app: Application?) {

    }

    override fun onEnable() {
        val ui = JavaFxUI.getInstance()
        ui.runInJavaFxThread {
            objects.start()
            ui.attachChild(tgtList)
        }
    }

    override fun onDisable() {
        val ui = JavaFxUI.getInstance()
        JavaFxUI.getInstance().runInJavaFxThread {
            ui.detachChild(tgtList)
            objects.stop()
        }
    }

    override fun update(tpf: Float) {
        timer += tpf
        if(timer < frameTime) return
        timer = 0f
        //update gui at approx 30fps for performance
        JavaFxUI.getInstance().runInJavaFxThread { updateUI() }
    }

    private fun updateUI(){
        objects.update()
        nearbyTgtList.refresh()
    }

    private inner class TargetListContainer(data:EntityData): EntityContainer<Target>(data, Name::class.java,
        Position::class.java, Velocity::class.java){
        override fun addObject(e: Entity): Target {
            val name = e.get(Name::class.java).name
            val pos = e.get(Position::class.java).position
            val vel = e.get(Velocity::class.java).velocity
            val target = Target(name, pos, vel)
            nearbyTgtList.items.add(target)
            return target
        }

        override fun updateObject(target: Target, e: Entity) {
            target.position = e.get(Position::class.java).position
            target.velocity = e.get(Velocity::class.java).velocity
        }

        override fun removeObject(target: Target?, e: Entity?) {
            nearbyTgtList.items.remove(target)
        }

    }

    private class Target(var name:String, var position: Vec3d, var velocity: Vec3d){
        override fun toString(): String {
            return "$name Pos:$position Vel:$velocity"
        }
    }
}