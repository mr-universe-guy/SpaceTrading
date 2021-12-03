package `fun`.familyfunforce.cosmos.ui

import `fun`.familyfunforce.cosmos.DataSystem
import `fun`.familyfunforce.cosmos.Position
import `fun`.familyfunforce.cosmos.SpaceTraderApp
import `fun`.familyfunforce.cosmos.Velocity
import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.simsilica.es.*
import com.simsilica.mathd.Vec3d
import io.tlf.jme.jfx.JavaFxUI
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.ListView
import javafx.scene.control.ScrollPane

class FlightUIState: BaseAppState() {
    private lateinit var tgtList: Node
    @FXML
    private lateinit var tgtListPane: ScrollPane
    private val nearbyTgtList = FXCollections.observableArrayList<Target>()
    private val tgtListComp = DistanceComparator()
    private val tgtListView = ListView(nearbyTgtList.sorted(tgtListComp))
    private lateinit var data: EntityData
    private lateinit var objects: TargetListContainer
    private val frameTime = 1f/24f
    private var timer = 0f
    private var playerPos: Vec3d? = null
    private var player: WatchedEntity? = null

    override fun initialize(_app: Application?) {
        val app = _app as SpaceTraderApp
        data = app.manager.get(DataSystem::class.java).getPhysicsData()
        JavaFxUI.getInstance().runInJavaFxThread {
            objects = TargetListContainer(data)
            val loader = FXMLLoader(javaClass.getResource("/UI/TestTargetsList.fxml" ))
            loader.setController(this)
            tgtList = loader.load()
        }
    }

    /**
     * Called by javaFX upon loading scene from fxml
     */
    fun initialize(){
        tgtListPane.content = tgtListView
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

    fun setPlayerId(id:EntityId?){
        if(id == null){
            player?.release()
            player = null
            playerPos = null
            return
        }
        player = data.watchEntity(id, Position::class.java)
    }

    override fun update(tpf: Float) {
        timer += tpf
        if(timer < frameTime) return
        timer = 0f
        //update player position
        if(player?.applyChanges() == true){
            playerPos = player?.get(Position::class.java)?.position
        }
        //update gui at approx 30fps for performance
        JavaFxUI.getInstance().runInJavaFxThread { updateUI() }
    }

    private fun updateUI(){
        objects.update()
        tgtListView.refresh()
    }

    private inner class TargetListContainer(data:EntityData): EntityContainer<Target>(data, Name::class.java,
        Position::class.java, Velocity::class.java){
        override fun addObject(e: Entity): Target {
            val name = e.get(Name::class.java).name
            val pos = e.get(Position::class.java).position
            val vel = e.get(Velocity::class.java).velocity
            val target = Target(name, pos, vel)
            nearbyTgtList.add(target)
            return target
        }

        override fun updateObject(target: Target, e: Entity) {
            target.position = e.get(Position::class.java).position
            target.velocity = e.get(Velocity::class.java).velocity
            nearbyTgtList[nearbyTgtList.indexOf(target)] = target
        }

        override fun removeObject(target: Target?, e: Entity?) {
            nearbyTgtList.remove(target)
        }

    }

    private inner class Target(var name:String, var position: Vec3d, var velocity: Vec3d){
        override fun toString(): String {
            val ppos = playerPos?: Vec3d.ZERO
            return "$name Dist:${position.distance(ppos)}"
        }
    }

    private inner class DistanceComparator: Comparator<Target>{
        override fun compare(p0: Target?, p1: Target?): Int {
            val position = playerPos?: Vec3d.ZERO
            return compareValues(position.distanceSq(p0?.position), position.distanceSq(p1?.position))
        }
    }
}