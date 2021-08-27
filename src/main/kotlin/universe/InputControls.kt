package universe

import com.jme3.input.KeyInput
import com.simsilica.lemur.input.*

const val CAM_INPUT_GROUP = "CamInput"
val CAM_INPUT_HORIZONTAL = FunctionId(CAM_INPUT_GROUP, "horiz_cam")
val CAM_INPUT_VERTICAL = FunctionId(CAM_INPUT_GROUP, "vert_cam")
val CAM_INPUT_ZOOM = FunctionId(CAM_INPUT_GROUP, "zoom_cam")

const val SHIP_INPUT_GROUP = "ShipInput"
val SHIP_NEXT_TARGET = FunctionId(SHIP_INPUT_GROUP, "next_target")

fun registerDefaults(mapper: InputMapper){
    //CAMERA
    mapper.map(CAM_INPUT_HORIZONTAL, Axis.MOUSE_X, Button.MOUSE_BUTTON2)
    mapper.map(CAM_INPUT_VERTICAL, Axis.MOUSE_Y, Button.MOUSE_BUTTON2)
    mapper.map(CAM_INPUT_ZOOM, Axis.MOUSE_WHEEL, Button.MOUSE_BUTTON2)
    //ship shortcuts
    mapper.map(SHIP_NEXT_TARGET, InputState.Positive, KeyInput.KEY_T)
}