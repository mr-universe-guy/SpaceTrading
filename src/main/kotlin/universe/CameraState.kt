package universe

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import com.jme3.scene.Spatial
import com.simsilica.es.EntityId
import com.simsilica.lemur.GuiGlobals
import com.simsilica.lemur.input.*

class CameraState: BaseAppState(), AnalogFunctionListener {
    private lateinit var visState: VisualState
    private lateinit var camera: Camera
    private var target: Spatial? = null
    private val minZoom = 1f
    private val maxZoom = 50f
    private val inputs = Vector3f(0f,0f,0.5f)
    private val rotation = Quaternion()

    override fun initialize(_app: Application) {
        val app = _app as SpaceTraderApp
        visState = getState(VisualState::class.java)
        camera = app.camera
        //default bindings for now
        GuiGlobals.getInstance().inputMapper.addAnalogListener(
                this, CAM_INPUT_HORIZONTAL, CAM_INPUT_VERTICAL, CAM_INPUT_ZOOM
            )
    }

    override fun cleanup(_app: Application) {
        GuiGlobals.getInstance().inputMapper.removeAnalogListener(this, CAM_INPUT_HORIZONTAL, CAM_INPUT_VERTICAL, CAM_INPUT_ZOOM)
    }

    override fun onEnable() {
        GuiGlobals.getInstance().inputMapper.activateGroup(CAM_INPUT_GROUP)
    }

    override fun onDisable() {
        GuiGlobals.getInstance().inputMapper.deactivateGroup(CAM_INPUT_GROUP)
    }

    /**
     * Set the target spatial for the camera, or null to return camera to origin
     */
    fun setTarget(id: EntityId?){
        if (id==null){
            target = null
            return
        }
        target = visState.getSpatialFromId(id)
    }

    override fun update(tpf: Float) {
        val pos = if(target != null) target!!.worldTranslation else Vector3f(0f,0f,0f)
        //Update the camera location and rotation around the spatial as the center point
        rotation.fromAngles(inputs.y, inputs.x, 0f)
        camera.location = pos.add(rotation.mult(Vector3f(0f, 0f, -(minZoom+(inputs.z*maxZoom)))))
        camera.rotation = rotation
    }

    override fun valueActive(func: FunctionId?, value: Double, tpf: Double) {
        if(CAM_INPUT_HORIZONTAL == func){
            inputs.x = ((inputs.x+value*tpf)%(Math.PI*2)).toFloat()
        }
        if(CAM_INPUT_VERTICAL == func){
            inputs.y = ((inputs.y+value*tpf)%(Math.PI*2)).toFloat()
        }
        if(CAM_INPUT_ZOOM == func){
            inputs.z = ((inputs.z+value*tpf).coerceIn(0.0, 1.0)).toFloat()
        }
    }
}