package `fun`.familyfunforce.cosmos.ui

import `fun`.familyfunforce.cosmos.SpaceTraderApp
import `fun`.familyfunforce.cosmos.VisualState
import `fun`.familyfunforce.cosmos.event.PlayerIdChangeEvent
import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import com.jme3.scene.Spatial
import com.simsilica.es.EntityId
import com.simsilica.event.EventBus
import com.simsilica.event.EventListener
import com.simsilica.event.EventType
import com.simsilica.lemur.input.*
import com.simsilica.mathd.Vec3d

class CameraManagerState(val cam:Camera): BaseAppState(), AnalogFunctionListener, StateFunctionListener, EventListener<PlayerIdChangeEvent>{
    /**
     * Stores the current camera axis inputs in a single variable
     */
    private val camAxis = Vec3d()
    private var camPressed = false
    var target: Spatial? = null
    var activeController: CameraController? = null
        set(value) {
            field = value
            value?.cam = cam
        }

    override fun initialize(app: Application?) {
        EventBus.addListener(PlayerIdChangeEvent.PlayerIdCreated, this)
        EventBus.addListener(PlayerIdChangeEvent.PlayerIdChanged, this)
    }

    override fun newEvent(type: EventType<PlayerIdChangeEvent>, event: PlayerIdChangeEvent) {
        application.enqueue { setTargetFromId(event.playerId) }
    }

    override fun cleanup(app: Application?) {
        EventBus.removeListener(PlayerIdChangeEvent.PlayerIdCreated, this)
        EventBus.removeListener(PlayerIdChangeEvent.PlayerIdChanged, this)
    }

    /**
     * Set the target spatial from an entity ID.
     */
    fun setTargetFromId(id: EntityId){
        target = getState(VisualState::class.java)!!.getSpatialFromId(id)
    }

    override fun onEnable() {
        val mapper = (application as SpaceTraderApp).manager.get(InputMapper::class.java)
        mapper.addAnalogListener(this, CAM_INPUT_YAW, CAM_INPUT_PITCH, CAM_INPUT_ZOOM)
        mapper.addStateListener(this, CAM_INPUT_HOLDTOROTATE)
        mapper.activateGroup(CAM_INPUT_GROUP)
    }

    override fun onDisable() {
        val mapper = (application as SpaceTraderApp).manager.get(InputMapper::class.java)
        mapper.removeAnalogListener(this, CAM_INPUT_YAW, CAM_INPUT_PITCH, CAM_INPUT_ZOOM)
        mapper.removeStateListener(this, CAM_INPUT_HOLDTOROTATE)
    }

    override fun update(tpf: Float) {
        activeController?.let { con ->
            target?.let { con.targetPos.set(it.worldTranslation) }
            con.updateCamera(tpf, camAxis.toVector3f(), camPressed)
        }
        camAxis.set(0.0,0.0,0.0)
    }

    override fun valueActive(func: FunctionId?, value: Double, tpf: Double) {
        if(CAM_INPUT_YAW == func){
            camAxis.x = value
        }
        if(CAM_INPUT_PITCH == func){
            camAxis.y = value
        }
        if(CAM_INPUT_ZOOM == func){
            camAxis.z = value*tpf*10
        }
    }

    override fun valueChanged(func: FunctionId?, value: InputState, tpf: Double) {
        if(CAM_INPUT_HOLDTOROTATE == func){
            camPressed = InputState.Positive == value
        }
    }
}

interface CameraController{
    var cam:Camera?
    val targetPos: Vector3f

    /**
     * @param tpf
     * @param inputRotation Vec3d where X=Camera Horizontal, y=Camera Vertical, z=Camera Zoom
     * @param camPressed true when camera's HoldToRotate button is active
     */
    fun updateCamera(tpf:Float, inputRotation:Vector3f, camPressed:Boolean)
}

class OrbitController(private val minZoom:Float, private val maxZoom:Float, val speed:Float): CameraController{
    override val targetPos = Vector3f()
    override var cam:Camera?=null
    private val offset = Vector3f(0f,0f,0.5f)
    private val rotation = Quaternion()

    override fun updateCamera(tpf: Float, inputRotation: Vector3f, camPressed: Boolean) {
        if(camPressed) {
            offset.x = (offset.x+(inputRotation.x*tpf*speed)) % FastMath.TWO_PI
            offset.y = (offset.y+(inputRotation.y*tpf*speed)) % FastMath.TWO_PI
            offset.z = (offset.z+(inputRotation.z*tpf*speed)).coerceIn(0f,1f)
            rotation.fromAngles(offset.y, offset.x,0f)
        }
        cam?.location = targetPos.add(rotation.mult(Vector3f(0f,0f,-(minZoom+(offset.z*maxZoom)))))
        cam?.rotation = rotation
    }
}