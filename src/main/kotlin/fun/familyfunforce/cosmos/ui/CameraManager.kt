package `fun`.familyfunforce.cosmos.ui

import `fun`.familyfunforce.cosmos.SpaceTraderApp
import `fun`.familyfunforce.cosmos.VisualState
import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import com.jme3.scene.Spatial
import com.simsilica.es.EntityId
import com.simsilica.lemur.input.*
import com.simsilica.mathd.Vec3d

class CameraManagerState(val cam:Camera): BaseAppState(), AnalogFunctionListener, StateFunctionListener {

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

    }

    override fun cleanup(app: Application?) {

    }

    /**
     * Set the target spatial from an entity ID.
     */
    fun setTargetFromId(id: EntityId){
        target = getState(VisualState::class.java).getSpatialFromId(id)
        println("Target set to $target")
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

class OrbitController(private var minZoom:Float, private var maxZoom:Float): CameraController{
    override val targetPos = Vector3f()
    override var cam:Camera?=null
    private val offset = Vector3f(0f,0f,0.5f)
    private val rotation = Quaternion()

    override fun updateCamera(tpf: Float, inputRotation: Vector3f, camPressed: Boolean) {
        if(camPressed) {
            offset.x = ((offset.x+inputRotation.x*tpf)% FastMath.TWO_PI)
            offset.y = ((offset.y+inputRotation.y*tpf)%FastMath.TWO_PI)
            offset.z = (offset.z+inputRotation.z*tpf).coerceIn(0f,1f)
            rotation.fromAngles(offset.y, offset.x,0f)
        }
        cam?.location = targetPos.add(rotation.mult(Vector3f(0f,0f,-(minZoom+(offset.z*maxZoom)))))
        cam?.rotation = rotation
    }
}